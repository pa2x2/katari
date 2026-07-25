package eu.kanade.tachiyomi.ui.browse.catalog

import eu.kanade.tachiyomi.source.entry.EntryFilterList
import mihon.entry.interactions.EntryCatalogueFeature
import mihon.entry.interactions.EntryCatalogueFiltersResult

/**
 * Loads and resolves filter lists for catalogue sources.
 */
class CatalogFilterLoader(
    private val catalogueFeature: EntryCatalogueFeature,
) {

    /**
     * Resolves the current filter list for the source.
     */
    suspend fun load(sourceId: Long): EntryFilterList {
        return when (val result = catalogueFeature.filters(sourceId)) {
            is EntryCatalogueFiltersResult.Available -> result.filters
            is EntryCatalogueFiltersResult.Unavailable -> EntryFilterList()
            is EntryCatalogueFiltersResult.Failed -> throw result.cause
        }
    }
}
