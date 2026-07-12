package tachiyomi.domain.source.repository

import androidx.paging.PagingSource
import eu.kanade.tachiyomi.source.entry.EntryFilterList
import tachiyomi.domain.source.model.CatalogListItem

typealias CatalogPagingSource = PagingSource<Long, CatalogListItem>

interface CatalogSourceRepository {

    fun search(sourceId: Long, query: String, filterList: EntryFilterList): CatalogPagingSource

    fun getPopular(sourceId: Long): CatalogPagingSource

    fun getLatest(sourceId: Long): CatalogPagingSource
}
