package mihon.entry.interactions.catalogue.runtime

import androidx.paging.PagingSource
import eu.kanade.tachiyomi.source.entry.EntryFilter
import eu.kanade.tachiyomi.source.entry.EntryFilterNavigationRequest
import eu.kanade.tachiyomi.source.entry.EntryFilterPageItem
import eu.kanade.tachiyomi.source.entry.EntryFilterTextInput
import kotlinx.coroutines.CancellationException
import mihon.entry.interactions.catalogue.EntryCatalogueBrowseRequest
import mihon.entry.interactions.catalogue.EntryCatalogueFeature
import mihon.entry.interactions.catalogue.EntryCatalogueFilterNavigationResult
import mihon.entry.interactions.catalogue.EntryCatalogueFilterSuggestionsResult
import mihon.entry.interactions.catalogue.EntryCatalogueFiltersResult
import mihon.entry.interactions.catalogue.EntryCatalogueListing
import mihon.entry.interactions.catalogue.EntryCataloguePagedFilterRequest
import mihon.entry.interactions.catalogue.EntryCatalogueSearchRequest
import mihon.entry.interactions.catalogue.EntryCatalogueSearchResult
import mihon.entry.interactions.catalogue.EntryCatalogueSourceInfo
import mihon.entry.interactions.catalogue.EntryCatalogueSourceResolution
import mihon.entry.interactions.catalogue.EntryCatalogueUnavailableReason
import mihon.entry.interactions.catalogue.host.EntryCatalogueHostSource
import mihon.entry.interactions.catalogue.host.EntryCatalogueHostSourceResolution
import mihon.entry.interactions.catalogue.host.EntryCatalogueProviderHost
import mihon.entry.interactions.catalogue.paging.EntryCatalogueFilterPagingSource
import mihon.entry.interactions.catalogue.paging.EntryCataloguePagingSource
import tachiyomi.domain.entry.adapter.toEntry
import tachiyomi.domain.entry.interactor.NetworkToLocalEntry
import tachiyomi.domain.entry.model.identity
import tachiyomi.domain.source.model.EntrySourceDescription

internal class DefaultEntryCatalogueFeature(
    private val host: EntryCatalogueProviderHost,
    private val graphStateValidator: EntryCatalogueGraphStateValidator,
    private val networkToLocalEntry: NetworkToLocalEntry,
) : EntryCatalogueFeature {
    override val isInitialized = host.isInitialized

    override fun sources(): List<EntryCatalogueSourceInfo> {
        return host.sources().map { source ->
            graphStateValidator.validate(source.description)
            source.toInfo()
        }
    }

    override fun source(sourceId: Long): EntryCatalogueSourceResolution {
        return when (val resolution = host.source(sourceId)) {
            is EntryCatalogueHostSourceResolution.Available -> {
                graphStateValidator.validate(resolution.source.description)
                EntryCatalogueSourceResolution.Available(resolution.source.toInfo())
            }
            EntryCatalogueHostSourceResolution.Missing -> EntryCatalogueSourceResolution.Missing(sourceId)
            EntryCatalogueHostSourceResolution.Unsupported -> EntryCatalogueSourceResolution.Unsupported(sourceId)
        }
    }

    override fun description(sourceId: Long): EntrySourceDescription {
        return host.describe(sourceId).also(graphStateValidator::validate)
    }

    override suspend fun filters(sourceId: Long): EntryCatalogueFiltersResult {
        val source = when (val resolution = source(sourceId)) {
            is EntryCatalogueSourceResolution.Available -> resolution.source
            is EntryCatalogueSourceResolution.Missing -> {
                return EntryCatalogueFiltersResult.Unavailable(EntryCatalogueUnavailableReason.SOURCE_MISSING)
            }
            is EntryCatalogueSourceResolution.Unsupported -> {
                return EntryCatalogueFiltersResult.Unavailable(EntryCatalogueUnavailableReason.CATALOGUE_UNSUPPORTED)
            }
        }
        return try {
            EntryCatalogueFiltersResult.Available(host.filters(source.id))
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            EntryCatalogueFiltersResult.Failed(error)
        }
    }

    override suspend fun filterSuggestions(
        sourceId: Long,
        filter: EntryFilter.Autocomplete,
        input: EntryFilterTextInput,
    ): EntryCatalogueFilterSuggestionsResult {
        val source = when (val resolution = source(sourceId)) {
            is EntryCatalogueSourceResolution.Available -> resolution.source
            is EntryCatalogueSourceResolution.Missing -> {
                return EntryCatalogueFilterSuggestionsResult.Unavailable(
                    EntryCatalogueUnavailableReason.SOURCE_MISSING,
                )
            }
            is EntryCatalogueSourceResolution.Unsupported -> {
                return EntryCatalogueFilterSuggestionsResult.Unavailable(
                    EntryCatalogueUnavailableReason.CATALOGUE_UNSUPPORTED,
                )
            }
        }
        return try {
            val query = filter.getSuggestionQuery(input)
                ?.takeIf { it.length >= filter.options.minimumQueryLength }
                ?: return EntryCatalogueFilterSuggestionsResult.NotApplicable
            val suggestions = host.filterSuggestions(
                sourceId = source.id,
                filter = filter,
                input = input,
                query = query,
            )
                .distinctBy { it.id }
                .take(filter.options.maximumResults)
            EntryCatalogueFilterSuggestionsResult.Available(suggestions)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            EntryCatalogueFilterSuggestionsResult.Failed(error)
        }
    }

    override suspend fun filterNavigation(
        sourceId: Long,
        filter: EntryFilter.PagedGroup<*>,
        request: EntryFilterNavigationRequest,
    ): EntryCatalogueFilterNavigationResult {
        val source = when (val resolution = source(sourceId)) {
            is EntryCatalogueSourceResolution.Available -> resolution.source
            is EntryCatalogueSourceResolution.Missing -> {
                return EntryCatalogueFilterNavigationResult.Unavailable(
                    EntryCatalogueUnavailableReason.SOURCE_MISSING,
                )
            }
            is EntryCatalogueSourceResolution.Unsupported -> {
                return EntryCatalogueFilterNavigationResult.Unavailable(
                    EntryCatalogueUnavailableReason.CATALOGUE_UNSUPPORTED,
                )
            }
        }
        return try {
            EntryCatalogueFilterNavigationResult.Available(
                host.filterNavigation(source.id, filter, request),
            )
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            EntryCatalogueFilterNavigationResult.Failed(error)
        }
    }

    override fun filterItems(
        request: EntryCataloguePagedFilterRequest,
    ): PagingSource<String, EntryFilterPageItem> {
        return EntryCatalogueFilterPagingSource(
            request = request,
            host = host,
            sourceResolution = source(request.sourceId),
        )
    }

    override fun paging(request: EntryCatalogueBrowseRequest) = EntryCataloguePagingSource(
        request = request,
        host = host,
        sourceResolution = source(request.sourceId),
        networkToLocalEntry = networkToLocalEntry,
    )

    override suspend fun search(request: EntryCatalogueSearchRequest): EntryCatalogueSearchResult {
        val source = when (val resolution = source(request.sourceId)) {
            is EntryCatalogueSourceResolution.Available -> resolution.source
            is EntryCatalogueSourceResolution.Missing -> {
                return EntryCatalogueSearchResult.Unavailable(EntryCatalogueUnavailableReason.SOURCE_MISSING)
            }
            is EntryCatalogueSourceResolution.Unsupported -> {
                return EntryCatalogueSearchResult.Unavailable(EntryCatalogueUnavailableReason.CATALOGUE_UNSUPPORTED)
            }
        }
        return try {
            val entries = host.page(
                sourceId = source.id,
                page = 1,
                listing = EntryCatalogueListing.Search(request.query, filters = host.backgroundFilters(source.id)),
            ).items
                .map { it.toEntry(source.id) }
                .filter { request.requiredType == null || it.type == request.requiredType }
                .distinctBy { it.identity() }
            EntryCatalogueSearchResult.Success(entries)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            EntryCatalogueSearchResult.Failed(error)
        }
    }

    private fun EntryCatalogueHostSource.toInfo(): EntryCatalogueSourceInfo {
        val catalogue = checkNotNull(description.catalogue)
        return EntryCatalogueSourceInfo(
            id = id,
            name = name,
            language = description.language,
            supportedEntryTypes = description.supportedEntryTypes,
            itemOrientation = description.itemOrientation,
            supportsLatest = catalogue.supportsLatest,
        )
    }
}
