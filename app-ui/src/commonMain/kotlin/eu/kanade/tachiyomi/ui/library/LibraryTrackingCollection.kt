package eu.kanade.tachiyomi.ui.library

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import mihon.entry.interactions.tracking.EntryTrackingCollectionSnapshot
import tachiyomi.core.common.preference.TriState
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.library.model.LibrarySort
import tachiyomi.domain.library.model.effectiveLibrarySort

private val noTracking = EntryTrackingCollectionSnapshot(
    services = emptyList(),
    scoreSupportedEntryTypes = emptySet(),
    entries = emptyMap(),
)

/**
 * Tracks are only read by tracker filters and the tracker score sort, and loading every track is slow on large
 * libraries, so the collection is observed only while one of them is in use.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal fun observeLibraryTrackingCollection(
    trackingFilters: Flow<Map<Long, TriState>>,
    categories: Flow<List<Category>>,
    globalSort: Flow<LibrarySort>,
    collection: () -> Flow<EntryTrackingCollectionSnapshot>,
): Flow<EntryTrackingCollectionSnapshot> {
    return combine(trackingFilters, categories, globalSort) { filters, categories, globalSort ->
        filters.values.any { it != TriState.DISABLED } ||
            globalSort.type == LibrarySort.Type.TrackerMean ||
            categories.any { it.effectiveLibrarySort(globalSort).type == LibrarySort.Type.TrackerMean }
    }
        .distinctUntilChanged()
        .flatMapLatest { needsTracks -> if (needsTracks) collection() else flowOf(noTracking) }
}
