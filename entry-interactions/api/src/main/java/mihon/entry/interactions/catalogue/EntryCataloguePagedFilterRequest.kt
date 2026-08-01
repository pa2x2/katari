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
    val initialAnchor: String? = null,
) {
    init {
        require(initialLoadReason != EntryFilterPageLoadReason.PAGINATION) {
            "initialLoadReason must start a paging generation"
        }
        require(initialAnchor == null || initialAnchor.isNotEmpty()) { "initialAnchor must not be empty" }
    }
}
