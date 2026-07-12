package eu.kanade.tachiyomi.ui.browse.catalog

import eu.kanade.tachiyomi.source.entry.EntryFilterList
import eu.kanade.tachiyomi.source.hasAsyncFilters
import eu.kanade.tachiyomi.source.resolveFilterList
import tachiyomi.domain.source.service.SourceManager

/**
 * Loads and resolves filter lists for catalogue sources.
 */
class CatalogFilterLoader(
    private val sourceManager: SourceManager,
) {

    /**
     * Returns true if the source needs asynchronous filter loading before a search can be issued.
     */
    fun hasAsyncFilters(sourceId: Long): Boolean {
        return sourceManager.getCatalogueSource(sourceId)?.hasAsyncFilters() == true
    }

    /**
     * Resolves the current filter list for the source, suspending only when async filters are used.
     */
    suspend fun load(sourceId: Long): EntryFilterList {
        return sourceManager.getCatalogueSource(sourceId)?.resolveFilterList() ?: EntryFilterList()
    }
}
