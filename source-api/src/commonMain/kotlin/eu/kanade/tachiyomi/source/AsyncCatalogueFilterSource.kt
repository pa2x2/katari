package eu.kanade.tachiyomi.source

import eu.kanade.tachiyomi.source.model.FilterList

/**
 * Optional catalogue-source capability for sources whose filter metadata must be loaded asynchronously.
 */
interface AsyncCatalogueFilterSource : CatalogueSource {
    suspend fun getFilterListAsync(): FilterList
}
