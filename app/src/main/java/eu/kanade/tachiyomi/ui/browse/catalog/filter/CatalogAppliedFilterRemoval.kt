package eu.kanade.tachiyomi.ui.browse.catalog

import eu.kanade.tachiyomi.source.filter.detachedCopy
import eu.kanade.tachiyomi.ui.browse.source.browse.filter.resetFilterToDefault
import eu.kanade.tachiyomi.ui.browse.source.browse.filterAt

/**
 * Resets one applied filter value to its source default and re-runs the search.
 *
 * When the editor still matches the applied search it follows the removal; otherwise the user's unapplied edits are
 * left alone and only the results change.
 */
internal fun CatalogScreenModel.State.withoutAppliedFilter(path: List<Int>): CatalogScreenModel.State {
    val search = listing as? CatalogScreenModel.Listing.Search ?: return this
    val appliedFilters = search.filters.detachedCopy()
    val appliedTarget = appliedFilters.filterAt(path) ?: return this
    resetFilterToDefault(appliedTarget, appliedFilters, defaultFilters)
    val next = copy(listing = search.copy(filters = appliedFilters))
    if (hasUnappliedFilterChanges) return next
    val draftTarget = filters.filterAt(path) ?: return next
    resetFilterToDefault(draftTarget, filters, defaultFilters)
    return next.withRepublishedDraftFilters(filters)
}
