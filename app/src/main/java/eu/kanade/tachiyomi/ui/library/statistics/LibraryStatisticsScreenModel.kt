package eu.kanade.tachiyomi.ui.library.statistics

import androidx.compose.runtime.Immutable
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import logcat.LogPriority
import mihon.entry.interactions.download.EntryDownloadRuntimeFeature
import mihon.entry.interactions.tracking.EntryTrackingFeature
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.core.common.util.system.logcat
import tachiyomi.data.ActiveProfileProvider
import tachiyomi.domain.entry.interactor.GetLibraryEntries
import tachiyomi.domain.library.model.LibraryItem
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class LibraryStatisticsScreenModel(
    private val filter: LibraryStatisticsFilter,
    private val activeProfileProvider: ActiveProfileProvider = Injekt.get(),
    private val getLibraryEntries: GetLibraryEntries = Injekt.get(),
    private val downloadRuntime: EntryDownloadRuntimeFeature = Injekt.get(),
    private val trackingFeature: EntryTrackingFeature = Injekt.get(),
) : StateScreenModel<LibraryStatisticsScreenModel.State>(State.Loading) {

    init {
        screenModelScope.launchIO {
            activeProfileProvider.activeProfileIdFlow.collectLatest { profileId ->
                mutableState.update { State.Loading }
                val entries = getLibraryEntries.subscribe(profileId)
                val filteredItems = when (filter.kind) {
                    LibraryStatisticsFilterKind.LIBRARY -> entries.map { items ->
                        filterItems(items)
                    }
                    LibraryStatisticsFilterKind.OFFLINE -> combine(
                        entries,
                        downloadRuntime.changes.onStart { emit(Unit) },
                    ) { items, _ ->
                        filterItems(
                            items = items,
                            downloadCount = { item -> item.memberEntries.sumOf(downloadRuntime::downloadCount) },
                        )
                    }
                    LibraryStatisticsFilterKind.TRACKED -> combine(
                        entries,
                        trackingFeature.observeCollection(),
                    ) { items, tracking ->
                        filterItems(
                            items = items,
                            trackedEntryIds = tracking.entries.filterValues { it.isNotEmpty() }.keys,
                        )
                    }
                }
                filteredItems.catch { error ->
                    logcat(LogPriority.ERROR, error)
                    mutableState.update { State.Failed }
                }.collectLatest { items ->
                    mutableState.update { State.Success(items) }
                }
            }
        }
    }

    private fun filterItems(
        items: List<LibraryItem>,
        downloadCount: (LibraryItem) -> Int = { 0 },
        trackedEntryIds: Set<Long> = emptySet(),
    ): List<LibraryItem> = filterLibraryStatisticsItems(
        items = items.distinctBy { it.key },
        filter = filter,
        downloadCount = downloadCount,
        trackedEntryIds = trackedEntryIds,
    )

    sealed interface State {
        @Immutable
        data object Loading : State

        @Immutable
        data object Failed : State

        @Immutable
        data class Success(val items: List<LibraryItem>) : State
    }
}
