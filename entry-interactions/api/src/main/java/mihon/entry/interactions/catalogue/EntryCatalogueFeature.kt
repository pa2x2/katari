package mihon.entry.interactions.catalogue

import androidx.paging.PagingSource
import eu.kanade.tachiyomi.source.entry.EntryFilter
import eu.kanade.tachiyomi.source.entry.EntryFilterPageItem
import eu.kanade.tachiyomi.source.entry.EntryFilterTextInput
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

    suspend fun filterSuggestions(
        sourceId: Long,
        filter: EntryFilter.Autocomplete,
        input: EntryFilterTextInput,
    ): EntryCatalogueFilterSuggestionsResult

    fun filterItems(request: EntryCataloguePagedFilterRequest): PagingSource<String, EntryFilterPageItem>

    fun paging(request: EntryCatalogueBrowseRequest): PagingSource<Long, CatalogListItem>

    suspend fun search(request: EntryCatalogueSearchRequest): EntryCatalogueSearchResult
}
