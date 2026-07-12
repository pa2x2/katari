package eu.kanade.tachiyomi.ui.browse.feed

import androidx.paging.PagingSource
import eu.kanade.domain.source.model.FeedItemRef
import eu.kanade.domain.source.model.applySnapshot
import eu.kanade.domain.source.service.BrowseFeedService
import eu.kanade.domain.source.service.ProfileSourcePreferences
import eu.kanade.tachiyomi.source.entry.EntryFilterList
import eu.kanade.tachiyomi.source.entry.EntryItemOrientation
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.sourceItemOrientation
import eu.kanade.tachiyomi.ui.browse.catalog.CatalogFilterLoader
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import tachiyomi.domain.entry.interactor.GetEntry
import tachiyomi.domain.source.interactor.GetRemoteCatalog
import tachiyomi.domain.source.model.CatalogListItem
import tachiyomi.domain.source.service.SourceManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class CatalogChronologicalFeedScreenModel(
    profileId: Long,
    feedId: String,
    private val sourceId: Long,
    listingQuery: String?,
    initialFilterSnapshot: List<eu.kanade.domain.source.model.FilterStateNode>,
    chronological: Boolean = true,
    browseFeedService: BrowseFeedService = Injekt.get(),
    profileSourcePreferences: ProfileSourcePreferences = Injekt.get(),
    private val sourceManager: SourceManager = Injekt.get(),
    private val getRemoteCatalog: GetRemoteCatalog = Injekt.get(),
    private val getEntry: GetEntry = Injekt.get(),
) : FeedScreenModel<CatalogListItem>(
    feedId = feedId,
    listingQuery = listingQuery,
    initialFilterSnapshot = initialFilterSnapshot,
    chronological = chronological,
    browseFeedService = browseFeedService.forProfile(profileId),
    hideInLibraryItems = profileSourcePreferences.forProfile(profileId).hideInLibraryItems.get(),
) {

    private val filterLoader = CatalogFilterLoader(sourceManager)

    override suspend fun subscribeItem(ref: FeedItemRef): Flow<CatalogListItem> {
        return getEntry.subscribe(ref.id)
            .map { CatalogListItem.EntryItem(it, sourceItemOrientation) }
    }

    override suspend fun resolveFilters(): EntryFilterList {
        return filterLoader.load(sourceId).applySnapshot(initialFilterSnapshot)
    }

    override fun createPagingSource(filters: EntryFilterList): PagingSource<Long, CatalogListItem> {
        return getRemoteCatalog(
            sourceId = sourceId,
            query = listingQuery.orEmpty(),
            filterList = filters,
        )
    }

    override fun itemRef(item: CatalogListItem): FeedItemRef {
        return FeedItemRef(item.id, item.entryType)
    }

    override fun isItemInLibrary(item: CatalogListItem): Boolean {
        return item.favorite
    }

    override suspend fun filterNonLibraryRefs(refs: List<FeedItemRef>): List<FeedItemRef> {
        val nonFavoriteIds = buildSet {
            refs.map { it.id }
                .chunked(MAX_NON_FAVORITE_LOOKUP_SIZE)
                .forEach { chunk ->
                    addAll(getEntry.awaitNonFavoriteIds(chunk))
                }
        }

        return refs.filter { it.id in nonFavoriteIds }
    }

    private val sourceItemOrientation
        get() = sourceManager.getCatalogueSource(sourceId)?.sourceItemOrientation()
            ?: EntryItemOrientation.VERTICAL

    companion object {
        private const val MAX_NON_FAVORITE_LOOKUP_SIZE = 900
    }
}
