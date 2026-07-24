package mihon.entry.interactions

import androidx.paging.PagingSource
import kotlinx.coroutines.flow.StateFlow
import tachiyomi.domain.source.model.CatalogListItem
import tachiyomi.domain.source.model.EntrySourceDescription

/** Feature-owned application boundary for Catalogue availability and execution. */
interface EntryCatalogueFeature {
    val isInitialized: StateFlow<Boolean>

    fun sources(): List<EntryCatalogueSourceInfo>

    fun source(sourceId: Long): EntryCatalogueSourceResolution

    fun description(sourceId: Long): EntrySourceDescription

    suspend fun filters(sourceId: Long): EntryCatalogueFiltersResult

    fun paging(request: EntryCatalogueBrowseRequest): PagingSource<Long, CatalogListItem>

    suspend fun search(request: EntryCatalogueSearchRequest): EntryCatalogueSearchResult
}
