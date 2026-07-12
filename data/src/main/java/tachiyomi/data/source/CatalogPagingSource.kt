package tachiyomi.data.source

import androidx.paging.PagingState
import eu.kanade.tachiyomi.source.entry.EntryCatalogueSource
import eu.kanade.tachiyomi.source.entry.EntryFilterList
import eu.kanade.tachiyomi.source.entry.EntryItemOrientation
import eu.kanade.tachiyomi.source.entry.EntryPageResult
import eu.kanade.tachiyomi.source.entry.SEntry
import kotlinx.coroutines.CancellationException
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.domain.entry.adapter.toEntry
import tachiyomi.domain.entry.interactor.NetworkToLocalEntry
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryIdentity
import tachiyomi.domain.entry.model.identity
import tachiyomi.domain.source.model.CatalogListItem
import tachiyomi.domain.source.repository.CatalogPagingSource
import tachiyomi.domain.source.service.SourceManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class CatalogSearchPagingSource(
    sourceId: Long,
    sourceItemOrientation: EntryItemOrientation,
    sourceManager: SourceManager,
    private val query: String,
    private val filters: EntryFilterList,
    networkToLocalEntry: NetworkToLocalEntry = Injekt.get(),
) : BaseCatalogPagingSource(sourceId, sourceItemOrientation, sourceManager, networkToLocalEntry) {

    override suspend fun requestNextPage(currentPage: Int): CatalogPageResult {
        val source = sourceManager.getCatalogueSource(sourceId)
            ?: throw NoResultsException()
        return source.getSearchContent(currentPage, query, filters).toResult()
    }
}

class CatalogPopularPagingSource(
    sourceId: Long,
    sourceItemOrientation: EntryItemOrientation,
    sourceManager: SourceManager,
    networkToLocalEntry: NetworkToLocalEntry = Injekt.get(),
) : BaseCatalogPagingSource(sourceId, sourceItemOrientation, sourceManager, networkToLocalEntry) {

    override suspend fun requestNextPage(currentPage: Int): CatalogPageResult {
        val source = sourceManager.getCatalogueSource(sourceId)
            ?: throw NoResultsException()
        return source.getPopularContent(currentPage).toResult()
    }
}

class CatalogLatestPagingSource(
    sourceId: Long,
    sourceItemOrientation: EntryItemOrientation,
    sourceManager: SourceManager,
    networkToLocalEntry: NetworkToLocalEntry = Injekt.get(),
) : BaseCatalogPagingSource(sourceId, sourceItemOrientation, sourceManager, networkToLocalEntry) {

    override suspend fun requestNextPage(currentPage: Int): CatalogPageResult {
        val source = sourceManager.getCatalogueSource(sourceId)
            ?: throw NoResultsException()
        return source.getLatestUpdates(currentPage).toResult()
    }
}

abstract class BaseCatalogPagingSource(
    protected val sourceId: Long,
    private val sourceItemOrientation: EntryItemOrientation,
    protected val sourceManager: SourceManager,
    private val networkToLocalEntry: NetworkToLocalEntry,
) : CatalogPagingSource() {

    private val seenEntries = hashSetOf<EntryIdentity>()

    abstract suspend fun requestNextPage(currentPage: Int): CatalogPageResult

    override suspend fun load(params: LoadParams<Long>): LoadResult<Long, CatalogListItem> {
        val page = params.key ?: 1

        return try {
            val result = withIOContext {
                requestNextPage(page.toInt())
                    .takeIf { it.entries.isNotEmpty() }
                    ?: throw NoResultsException()
            }

            val entries = result.entries
                .filter { seenEntries.add(it.identity()) }
                .let { networkToLocalEntry(it) }

            val items = entries.map { CatalogListItem.EntryItem(it, sourceItemOrientation) }

            LoadResult.Page(
                data = items,
                prevKey = null,
                nextKey = if (result.hasNextPage) page + 1 else null,
            )
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Long, CatalogListItem>): Long? {
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey?.plus(1) ?: anchorPage?.nextKey?.minus(1)
        }
    }

    data class CatalogPageResult(
        val entries: List<Entry> = emptyList(),
        val hasNextPage: Boolean,
    )

    protected fun EntryPageResult<SEntry>.toResult(): CatalogPageResult {
        return CatalogPageResult(
            entries = items.map { it.toEntry(sourceId) },
            hasNextPage = hasNextPage,
        )
    }
}

class NoResultsException : Exception()
