package app.katari.sync.ui.handoff

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.katari.sync.client.handoff.EntryHandOff
import app.katari.sync.client.handoff.HandOffCheck
import app.katari.sync.client.handoff.ProgressArrival
import app.katari.sync.client.trigger.SyncTriggers
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactoryKey
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import mihon.app.di.AppUiScope
import kotlin.time.Clock

/**
 * An open entry's side of picking up on another device (D052): opening it fetches its progress from the other
 * devices first, and [note] says on Resume how that went.
 */
@AssistedInject
class EntryHandOffViewModel(
    @Assisted private val entryId: Long,
    triggers: SyncTriggers,
    handOff: EntryHandOff,
) : ViewModel() {

    @AssistedFactory
    @ManualViewModelAssistedFactoryKey
    @ContributesIntoMap(AppUiScope::class)
    interface Factory : ManualViewModelAssistedFactory {
        fun create(entryId: Long): EntryHandOffViewModel
    }

    // Newer progress taken in while the entry is open, and when
    private val arrived = MutableStateFlow<Pair<ProgressArrival, Long>?>(null)

    private val checkFinishedAt = MutableStateFlow(0L)

    val note: StateFlow<HandOffNote?> = combine(
        handOff.checks.map { it[entryId] }.distinctUntilChanged(),
        checkFinishedAt,
        arrived,
        ticks(),
    ) { check, finishedAt, arrival, now ->
        HandOffNote.of(check, arrival?.first, arrival?.second ?: finishedAt, now)
    }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), null)

    init {
        triggers.entryOpened(entryId)
        viewModelScope.launch {
            handOff.checks.map { it[entryId] }.distinctUntilChanged().collect { check ->
                if (check is HandOffCheck.Checked) checkFinishedAt.value = Clock.System.now().toEpochMilliseconds()
            }
        }
        viewModelScope.launch {
            handOff.arrivals.filter { it.entryId == entryId }.collect {
                arrived.value = it to Clock.System.now().toEpochMilliseconds()
            }
        }
    }

    // Often enough for the device's name to go once its minute is up
    private fun ticks() = flow {
        while (true) {
            emit(Clock.System.now().toEpochMilliseconds())
            delay(TICK)
        }
    }

    private companion object {
        const val STOP_TIMEOUT = 5_000L
        const val TICK = 5_000L
    }
}
