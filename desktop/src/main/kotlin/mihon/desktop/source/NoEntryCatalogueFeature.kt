package mihon.desktop.source

import androidx.paging.PagingSource
import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.source.entry.EntryFilter
import eu.kanade.tachiyomi.source.entry.EntryFilterNavigationRequest
import eu.kanade.tachiyomi.source.entry.EntryFilterPageItem
import eu.kanade.tachiyomi.source.entry.EntryFilterTextInput
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import mihon.entry.interactions.catalogue.EntryCatalogueBrowseRequest
import mihon.entry.interactions.catalogue.EntryCatalogueFeature
import mihon.entry.interactions.catalogue.EntryCatalogueFilterNavigationResult
import mihon.entry.interactions.catalogue.EntryCatalogueFilterSuggestionsResult
import mihon.entry.interactions.catalogue.EntryCatalogueFiltersResult
import mihon.entry.interactions.catalogue.EntryCataloguePagedFilterRequest
import mihon.entry.interactions.catalogue.EntryCatalogueSearchRequest
import mihon.entry.interactions.catalogue.EntryCatalogueSearchResult
import mihon.entry.interactions.catalogue.EntryCatalogueSourceInfo
import mihon.entry.interactions.catalogue.EntryCatalogueSourceResolution
import mihon.entry.interactions.catalogue.EntryCatalogueUnavailableReason
import tachiyomi.domain.source.model.CatalogListItem
import tachiyomi.domain.source.model.EntrySourceDescription
import tachiyomi.domain.source.service.SourceManager

/**
 * The catalogue feature as it answers without any source installed: no catalogues, every source missing, and each
 * source described as [NoSourceDescriptions] describes it. Stands in for the shared feature, which reaches sources
 * through Android's extension loading, until desktop loads extensions.
 */
@Inject
class NoEntryCatalogueFeature(
    private val sourceManager: SourceManager,
    private val sourceDescriptions: NoSourceDescriptions,
) : EntryCatalogueFeature {
    override val isInitialized: StateFlow<Boolean> = MutableStateFlow(true).asStateFlow()

    override fun sources(): List<EntryCatalogueSourceInfo> = emptyList()

    override fun source(sourceId: Long): EntryCatalogueSourceResolution =
        EntryCatalogueSourceResolution.Missing(sourceId)

    override fun description(sourceId: Long): EntrySourceDescription =
        sourceDescriptions.describe(sourceManager.getOrStub(sourceId))

    override suspend fun filters(sourceId: Long): EntryCatalogueFiltersResult =
        EntryCatalogueFiltersResult.Unavailable(missing)

    override suspend fun filterSuggestions(
        sourceId: Long,
        filter: EntryFilter.Autocomplete,
        input: EntryFilterTextInput,
    ): EntryCatalogueFilterSuggestionsResult = EntryCatalogueFilterSuggestionsResult.Unavailable(missing)

    override suspend fun filterNavigation(
        sourceId: Long,
        filter: EntryFilter.PagedGroup<*>,
        request: EntryFilterNavigationRequest,
    ): EntryCatalogueFilterNavigationResult = EntryCatalogueFilterNavigationResult.Unavailable(missing)

    override fun filterItems(request: EntryCataloguePagedFilterRequest): PagingSource<String, EntryFilterPageItem> =
        error("Desktop has no catalogues to page through")

    override fun paging(request: EntryCatalogueBrowseRequest): PagingSource<Long, CatalogListItem> =
        error("Desktop has no catalogues to page through")

    override suspend fun search(request: EntryCatalogueSearchRequest): EntryCatalogueSearchResult =
        EntryCatalogueSearchResult.Unavailable(missing)

    private companion object {
        val missing = EntryCatalogueUnavailableReason.SOURCE_MISSING
    }
}
