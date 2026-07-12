package tachiyomi.data.source

import eu.kanade.tachiyomi.source.entry.EntryFilterList
import eu.kanade.tachiyomi.source.entry.EntryItemOrientation
import eu.kanade.tachiyomi.source.entry.entryItemOrientation
import tachiyomi.domain.source.repository.CatalogPagingSource
import tachiyomi.domain.source.repository.CatalogSourceRepository
import tachiyomi.domain.source.service.SourceManager

class CatalogSourceRepositoryImpl(
    private val sourceManager: SourceManager,
) : CatalogSourceRepository {

    override fun search(sourceId: Long, query: String, filterList: EntryFilterList): CatalogPagingSource {
        return CatalogSearchPagingSource(
            sourceId = sourceId,
            sourceItemOrientation = getOrientation(sourceId),
            sourceManager = sourceManager,
            query = query,
            filters = filterList,
        )
    }

    override fun getPopular(sourceId: Long): CatalogPagingSource {
        return CatalogPopularPagingSource(
            sourceId = sourceId,
            sourceItemOrientation = getOrientation(sourceId),
            sourceManager = sourceManager,
        )
    }

    override fun getLatest(sourceId: Long): CatalogPagingSource {
        return CatalogLatestPagingSource(
            sourceId = sourceId,
            sourceItemOrientation = getOrientation(sourceId),
            sourceManager = sourceManager,
        )
    }

    private fun getOrientation(sourceId: Long): EntryItemOrientation {
        val source = sourceManager.get(sourceId)
        return source?.entryItemOrientation() ?: EntryItemOrientation.VERTICAL
    }
}
