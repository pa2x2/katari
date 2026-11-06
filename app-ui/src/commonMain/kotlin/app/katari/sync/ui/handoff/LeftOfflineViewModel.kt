package app.katari.sync.ui.handoff

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.katari.sync.client.status.SyncStatus
import app.katari.sync.ui.status.SyncStatusState
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactoryKey
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import mihon.app.di.AppUiScope
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds

/**
 * Coming back to an entry from its reader or player while sync can't send (D054): [savedOffline] fires when the entry
 * was read while its screen was covered and what was read stays on this device until it is online again. The host
 * says when the screen is covered and shown with [onHidden] and [onShown].
 */
@AssistedInject
class LeftOfflineViewModel(
    @Assisted private val entryId: Long,
    private val settledReading: SettledReading,
    statusState: SyncStatusState,
) : ViewModel() {

    @AssistedFactory
    @ManualViewModelAssistedFactoryKey
    @ContributesIntoMap(AppUiScope::class)
    interface Factory : ManualViewModelAssistedFactory {
        fun create(entryId: Long): LeftOfflineViewModel
    }

    private val status = statusState.status

    // Whether sync last ended unable to send, from the last status that wasn't a run going
    private var lastCouldntSend = false

    // When the entry's screen was last covered, as by a reader; null while it shows
    private var hiddenAt: Long? = null

    private var shownCheck: Job? = null

    private val savedOfflineEvents = Channel<Unit>(Channel.CONFLATED)

    val savedOffline: Flow<Unit> = savedOfflineEvents.receiveAsFlow()

    init {
        viewModelScope.launch {
            status.collect { status ->
                if (status != null && status.activity !is SyncStatus.Activity.Syncing) {
                    lastCouldntSend = status.couldntSend()
                }
            }
        }
    }

    fun onHidden() {
        shownCheck?.cancel()
        hiddenAt = Clock.System.now().toEpochMilliseconds()
    }

    fun onShown() {
        val since = hiddenAt ?: return
        hiddenAt = null
        shownCheck = viewModelScope.launch {
            // A reader records its last reading as it closes, which can land just after this screen shows again
            withTimeoutOrNull(SETTLE_WAIT) {
                settledReading.lastSettled.first { (it[entryId] ?: 0L) >= since }
            } ?: return@launch
            // Recording it starts a run to send it; its outcome says whether it went, or else what sync last knew
            val ended = withTimeoutOrNull(OUTCOME_WAIT) {
                status.first { it != null && it.activity !is SyncStatus.Activity.Syncing }
            }
            if (ended?.couldntSend() ?: lastCouldntSend) savedOfflineEvents.trySend(Unit)
        }
    }

    private fun SyncStatus.couldntSend(): Boolean = link is SyncStatus.Link.Linked &&
        (activity is SyncStatus.Activity.Offline || activity is SyncStatus.Activity.Unreachable)

    private companion object {
        val SETTLE_WAIT = 3.seconds
        val OUTCOME_WAIT = 10.seconds
    }
}
