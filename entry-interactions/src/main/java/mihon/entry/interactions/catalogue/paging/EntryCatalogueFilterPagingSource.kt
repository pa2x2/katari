package mihon.entry.interactions.catalogue.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import eu.kanade.tachiyomi.source.entry.EntryFilterPageItem
import eu.kanade.tachiyomi.source.entry.EntryFilterPageLoadReason
import eu.kanade.tachiyomi.source.entry.EntryFilterPageRequest
import kotlinx.coroutines.CancellationException
import mihon.entry.interactions.catalogue.EntryCataloguePagedFilterRequest
import mihon.entry.interactions.catalogue.EntryCatalogueSourceResolution
import mihon.entry.interactions.catalogue.EntryCatalogueUnavailableException
import mihon.entry.interactions.catalogue.EntryCatalogueUnavailableReason
import mihon.entry.interactions.catalogue.host.EntryCatalogueProviderHost
import tachiyomi.core.common.util.lang.withIOContext

internal class EntryCatalogueFilterPagingSource(
    private val request: EntryCataloguePagedFilterRequest,
    private val host: EntryCatalogueProviderHost,
    private val sourceResolution: EntryCatalogueSourceResolution,
) : PagingSource<String, EntryFilterPageItem>() {
    private val seenItemIds = hashSetOf<String>()

    override suspend fun load(params: LoadParams<String>): LoadResult<String, EntryFilterPageItem> {
        val continuationToken = params.key
        return try {
            when (sourceResolution) {
                is EntryCatalogueSourceResolution.Available -> Unit
                is EntryCatalogueSourceResolution.Missing -> throw EntryCatalogueUnavailableException(
                    request.sourceId,
                    EntryCatalogueUnavailableReason.SOURCE_MISSING,
                )
                is EntryCatalogueSourceResolution.Unsupported -> throw EntryCatalogueUnavailableException(
                    request.sourceId,
                    EntryCatalogueUnavailableReason.CATALOGUE_UNSUPPORTED,
                )
            }
            val requestedSize = request.filter.options.pageSize
            val pageRequest = EntryFilterPageRequest(
                scope = request.scope,
                query = request.query,
                continuationToken = continuationToken,
                requestedSize = requestedSize,
                reason = if (continuationToken == null) {
                    request.initialLoadReason
                } else {
                    EntryFilterPageLoadReason.APPEND
                },
            )
            val page = withIOContext {
                host.filterPage(request.sourceId, request.filter, pageRequest)
            }
            check(page.items.size <= requestedSize) {
                "Paged filter returned ${page.items.size} items for a requested page size of $requestedSize"
            }
            check(page.items.distinctBy(EntryFilterPageItem::id).size == page.items.size) {
                "Paged filter returned duplicate item IDs within one page"
            }
            check(page.nextContinuationToken == null || page.nextContinuationToken != continuationToken) {
                "Paged filter returned the same continuation token"
            }

            LoadResult.Page(
                data = page.items.filter { seenItemIds.add(it.id) },
                prevKey = null,
                nextKey = page.nextContinuationToken,
            )
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            LoadResult.Error(error)
        }
    }

    override fun getRefreshKey(state: PagingState<String, EntryFilterPageItem>): String? = null
}
