package mihon.entry.interactions.catalogue.host

import eu.kanade.tachiyomi.source.entry.EntryCatalogueSource
import eu.kanade.tachiyomi.source.entry.EntryFilter
import eu.kanade.tachiyomi.source.entry.EntryFilterList
import eu.kanade.tachiyomi.source.entry.EntryFilterSuggestion
import eu.kanade.tachiyomi.source.entry.EntryFilterTextInput
import eu.kanade.tachiyomi.source.entry.EntryPageResult
import eu.kanade.tachiyomi.source.entry.SEntry
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import eu.kanade.tachiyomi.source.entry.entryItemOrientation
import eu.kanade.tachiyomi.source.entry.supportedEntryTypes
import mihon.entry.interactions.catalogue.EntryCatalogueListing
import mihon.entry.interactions.catalogue.EntryCatalogueUnavailableException
import mihon.entry.interactions.catalogue.EntryCatalogueUnavailableReason
import tachiyomi.domain.source.model.EntryCatalogueDescription
import tachiyomi.domain.source.model.EntrySourceDescription
import tachiyomi.domain.source.model.UnifiedStubSource
import tachiyomi.domain.source.service.SourceManager

internal class SourceManagerEntryCatalogueProviderHost(
    private val sourceManager: SourceManager,
) : EntryCatalogueProviderHost {
    override val isInitialized = sourceManager.isInitialized

    override fun sources(): List<EntryCatalogueHostSource> {
        return sourceManager.getAll().mapNotNull(::catalogueSource)
    }

    override fun source(sourceId: Long): EntryCatalogueHostSourceResolution {
        val source = sourceManager.get(sourceId) ?: return EntryCatalogueHostSourceResolution.Missing
        return catalogueSource(source)
            ?.let(EntryCatalogueHostSourceResolution::Available)
            ?: EntryCatalogueHostSourceResolution.Unsupported
    }

    override fun describe(sourceId: Long): EntrySourceDescription {
        return describe(sourceManager.getOrStub(sourceId))
    }

    override fun describe(source: UnifiedSource): EntrySourceDescription {
        val catalogue = source as? EntryCatalogueSource
        return EntrySourceDescription(
            language = catalogue?.lang ?: (source as? UnifiedStubSource)?.lang.orEmpty(),
            supportedEntryTypes = source.supportedEntryTypes()?.toSet(),
            itemOrientation = source.entryItemOrientation(),
            catalogue = catalogue?.let { EntryCatalogueDescription(supportsLatest = it.supportsLatest) },
        )
    }

    override suspend fun filters(sourceId: Long): EntryFilterList {
        return catalogueProvider(sourceId).getFilterList()
    }

    override suspend fun filterSuggestions(
        sourceId: Long,
        filter: EntryFilter.Autocomplete,
        input: EntryFilterTextInput,
        query: String,
    ): List<EntryFilterSuggestion> {
        catalogueProvider(sourceId)
        return filter.getSuggestions(input, query)
    }

    override fun backgroundFilters(sourceId: Long): EntryFilterList {
        return catalogueProvider(sourceId).getFilterList()
    }

    override suspend fun page(
        sourceId: Long,
        page: Int,
        listing: EntryCatalogueListing,
    ): EntryPageResult<SEntry> {
        val source = catalogueProvider(sourceId)
        return when (listing) {
            EntryCatalogueListing.Popular -> source.getPopularContent(page)
            EntryCatalogueListing.Latest -> source.getLatestUpdates(page)
            is EntryCatalogueListing.Search -> source.getSearchContent(page, listing.query, listing.filters)
        }
    }

    private fun catalogueSource(source: UnifiedSource): EntryCatalogueHostSource? {
        if (source !is EntryCatalogueSource) return null
        return EntryCatalogueHostSource(
            id = source.id,
            name = source.name,
            description = describe(source),
        )
    }

    private fun catalogueProvider(sourceId: Long): UnifiedSource {
        val source = sourceManager.get(sourceId)
            ?: throw EntryCatalogueUnavailableException(sourceId, EntryCatalogueUnavailableReason.SOURCE_MISSING)
        if (source !is EntryCatalogueSource) {
            throw EntryCatalogueUnavailableException(sourceId, EntryCatalogueUnavailableReason.CATALOGUE_UNSUPPORTED)
        }
        return source
    }
}
