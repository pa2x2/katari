package eu.kanade.tachiyomi.ui.browse.catalog

import eu.kanade.tachiyomi.source.filter.detachedCopy

/**
 * Moves between source listings without discarding the applied search.
 *
 * Leaving a search for Popular or Latest parks it so the filter chip can return to it in one tap; the draft editor
 * is left untouched so nothing the user configured is lost.
 */
internal fun CatalogViewModel.State.switchListing(target: CatalogViewModel.Listing): CatalogViewModel.State {
    val parked = when {
        target is CatalogViewModel.Listing.Search -> null
        listing is CatalogViewModel.Listing.Search -> listing
        else -> parkedSearch
    }
    return copy(
        listing = target,
        parkedSearch = parked,
        toolbarQuery = (target as? CatalogViewModel.Listing.Search)?.query,
    )
}

/** The parked search the filter chip restores, present only while another listing is shown. */
internal val CatalogViewModel.State.restorableSearch: CatalogViewModel.Listing.Search?
    get() = parkedSearch.takeIf { listing !is CatalogViewModel.Listing.Search }

/**
 * Runs a toolbar search with the filters that are already applied.
 *
 * The draft is only committed through the validated apply action, so an unapplied or invalid draft never reaches
 * the source from here. Outside a search, the source defaults are the applied filters.
 */
internal fun CatalogViewModel.State.searchWithAppliedFilters(query: String): CatalogViewModel.State {
    val appliedFilters = (listing as? CatalogViewModel.Listing.Search)?.filters ?: defaultFilters
    return copy(
        listing = CatalogViewModel.Listing.Search(query = query, filters = appliedFilters.detachedCopy()),
        parkedSearch = null,
        toolbarQuery = query,
    )
}
