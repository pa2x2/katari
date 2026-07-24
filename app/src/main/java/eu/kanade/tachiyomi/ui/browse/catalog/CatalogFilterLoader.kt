package eu.kanade.tachiyomi.ui.browse.catalog

import eu.kanade.tachiyomi.source.entry.EntryFilterList
import mihon.entry.interactions.EntryCatalogueFeature
import mihon.entry.interactions.EntryCatalogueFiltersResult
import mihon.entry.interactions.EntryCatalogueSourceResolution

/**
 * Loads and resolves filter lists for catalogue sources.
 */
class CatalogFilterLoader(
    private val catalogueFeature: EntryCatalogueFeature,
) {

    /**
     * Returns true if the source needs asynchronous filter loading before a search can be issued.
     */
    fun usesAsyncFilters(sourceId: Long): Boolean {
        return (catalogueFeature.source(sourceId) as? EntryCatalogueSourceResolution.Available)
            ?.source
            ?.usesAsyncFilters == true
    }

    /**
     * Resolves the current filter list for the source, suspending only when async filters are used.
     */
    suspend fun load(sourceId: Long): EntryFilterList {
        return when (val result = catalogueFeature.filters(sourceId)) {
            is EntryCatalogueFiltersResult.Available -> result.filters
            is EntryCatalogueFiltersResult.Unavailable -> EntryFilterList()
            is EntryCatalogueFiltersResult.Failed -> throw result.cause
        }
    }
}
