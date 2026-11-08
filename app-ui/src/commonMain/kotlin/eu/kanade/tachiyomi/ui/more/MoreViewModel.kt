package eu.kanade.tachiyomi.ui.more

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.katari.entry.interactions.download.EntryDownloadRuntimeFeature
import app.katari.ui.di.AppUiScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import eu.kanade.core.preference.asState
import eu.kanade.domain.base.IncognitoPreferences
import eu.kanade.tachiyomi.ui.stats.recap.delivery.StatisticsUnopenedYearRecap
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import tachiyomi.domain.library.service.LibraryPreferences

@Inject
@ViewModelKey
@ContributesIntoMap(AppUiScope::class)
class MoreViewModel(
    downloadRuntime: EntryDownloadRuntimeFeature,
    incognitoPreferences: IncognitoPreferences,
    libraryPreferences: LibraryPreferences,
    unopenedYearRecap: StatisticsUnopenedYearRecap,
) : ViewModel() {

    var downloadedOnly by libraryPreferences.downloadedOnly.asState(viewModelScope)
    var incognitoMode by incognitoPreferences.incognitoMode.asState(viewModelScope)

    val downloadQueueState: StateFlow<DownloadQueueState> = downloadRuntime.state
        .map { runtime ->
            val queueSize = runtime.queue.sumOf { it.items.size }
            when {
                queueSize == 0 -> DownloadQueueState.Stopped
                !runtime.isRunning -> DownloadQueueState.Paused(queueSize)
                else -> DownloadQueueState.Downloading(queueSize)
            }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            DownloadQueueState.Stopped,
        )

    val hasNewRecap: StateFlow<Boolean> = unopenedYearRecap.subscribe()
        .map { it != null }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)
}

sealed interface DownloadQueueState {
    data object Stopped : DownloadQueueState
    data class Paused(val pending: Int) : DownloadQueueState
    data class Downloading(val pending: Int) : DownloadQueueState
}
