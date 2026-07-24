package mihon.entry.interactions

import androidx.paging.PagingSource
import androidx.paging.PagingState
import kotlinx.coroutines.CancellationException
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.domain.entry.adapter.toEntry
import tachiyomi.domain.entry.interactor.NetworkToLocalEntry
import tachiyomi.domain.entry.model.EntryIdentity
import tachiyomi.domain.entry.model.identity
import tachiyomi.domain.source.model.CatalogListItem

internal class EntryCataloguePagingSource(
    private val request: EntryCatalogueBrowseRequest,
    private val host: EntryCatalogueProviderHost,
    private val sourceResolution: EntryCatalogueSourceResolution,
    private val networkToLocalEntry: NetworkToLocalEntry,
) : PagingSource<Long, CatalogListItem>() {
    private val seenEntries = hashSetOf<EntryIdentity>()

    override suspend fun load(params: LoadParams<Long>): LoadResult<Long, CatalogListItem> {
        val page = params.key ?: 1
        return try {
            val source = when (sourceResolution) {
                is EntryCatalogueSourceResolution.Available -> sourceResolution.source
                is EntryCatalogueSourceResolution.Missing -> throw EntryCatalogueUnavailableException(
                    request.sourceId,
                    EntryCatalogueUnavailableReason.SOURCE_MISSING,
                )
                is EntryCatalogueSourceResolution.Unsupported -> throw EntryCatalogueUnavailableException(
                    request.sourceId,
                    EntryCatalogueUnavailableReason.CATALOGUE_UNSUPPORTED,
                )
            }
            if (request.listing == EntryCatalogueListing.Latest && !source.supportsLatest) {
                throw EntryCatalogueUnavailableException(
                    request.sourceId,
                    EntryCatalogueUnavailableReason.LATEST_UNSUPPORTED,
                )
            }
            val result = withIOContext { host.page(request.sourceId, page.toInt(), request.listing) }
            if (result.items.isEmpty()) throw EntryCatalogueNoResultsException()

            val entries = result.items
                .map { it.toEntry(request.sourceId) }
                .filter { seenEntries.add(it.identity()) }
                .let { networkToLocalEntry(it) }
            LoadResult.Page(
                data = entries.map { CatalogListItem.EntryItem(it, source.itemOrientation) },
                prevKey = null,
                nextKey = if (result.hasNextPage) page + 1 else null,
            )
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            LoadResult.Error(error)
        }
    }

    override fun getRefreshKey(state: PagingState<Long, CatalogListItem>): Long? {
        return state.anchorPosition?.let { position ->
            val page = state.closestPageToPosition(position)
            page?.prevKey?.plus(1) ?: page?.nextKey?.minus(1)
        }
    }
}
