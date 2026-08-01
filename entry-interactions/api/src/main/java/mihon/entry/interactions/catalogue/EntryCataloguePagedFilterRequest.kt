package mihon.entry.interactions.catalogue

import eu.kanade.tachiyomi.source.entry.EntryFilter
import eu.kanade.tachiyomi.source.entry.EntryFilterPageLoadReason
import eu.kanade.tachiyomi.source.entry.EntryFilterPageScope

/** App request for one independently paged filter-group projection. */
data class EntryCataloguePagedFilterRequest(
    val sourceId: Long,
    val filter: EntryFilter.PagedGroup<*>,
    val scope: EntryFilterPageScope,
    val query: String?,
    val initialLoadReason: EntryFilterPageLoadReason = EntryFilterPageLoadReason.INITIAL,
) {
    init {
        require(initialLoadReason != EntryFilterPageLoadReason.APPEND) {
            "initialLoadReason must start a paging generation"
        }
    }
}
