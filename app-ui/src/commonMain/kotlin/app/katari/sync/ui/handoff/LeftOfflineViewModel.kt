package app.katari.sync.ui.handoff

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.katari.sync.client.status.SyncStatus
import app.katari.sync.ui.status.SyncStatusState
import app.katari.ui.di.AppUiScope
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
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

    // When the entry's screen was last covered, as by a reader; null while it shows
    private var hiddenAt: Long? = null

    private var shownCheck: Job? = null

    private val savedOfflineEvents = Channel<Unit>(Channel.CONFLATED)

    val savedOffline: Flow<Unit> = savedOfflineEvents.receiveAsFlow()

    fun onHidden() {
        shownCheck?.cancel()
        hiddenAt = Clock.System.now().toEpochMilliseconds()
    }

    fun onShown() {
        val since = hiddenAt ?: return
        hiddenAt = null
        shownCheck = viewModelScope.launch {
            // A reader records its last reading as it stops, which Android may hold back until this screen has
            // settled after coming back, up to its idle timeout
            val settledAt = withTimeoutOrNull(SETTLE_WAIT) {
                settledReading.lastSettled.map { it[entryId] ?: 0L }.first { it >= since }
            } ?: return@launch
            // The status from before the reading was recorded says nothing about it: wait for sync to be unable to
            // send, or to finish a sync after it
            val couldntSend = withTimeoutOrNull(OUTCOME_WAIT) {
                status.mapNotNull { it?.leftOffline(settledAt) }.first()
            }
            if (couldntSend == true) savedOfflineEvents.trySend(Unit)
        }
    }

    /** True when sync can't send now, false once it synced after [settledAt], null while that isn't known yet. */
    private fun SyncStatus.leftOffline(settledAt: Long): Boolean? {
        if (link !is SyncStatus.Link.Linked) return false
        return when (val activity = activity) {
            is SyncStatus.Activity.Offline, is SyncStatus.Activity.Unreachable -> true
            is SyncStatus.Activity.UpToDate -> if ((activity.atMillis ?: 0L) >= settledAt) false else null
            else -> null
        }
    }

    private companion object {
        val SETTLE_WAIT = 15.seconds

        // Longer than a run takes to find the server silent ([app.katari.sync.client.server.SyncServerClients])
        val OUTCOME_WAIT = 45.seconds
    }
}
