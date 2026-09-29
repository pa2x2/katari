package eu.kanade.tachiyomi.ui.browse.catalog

import eu.kanade.tachiyomi.source.filter.detachedCopy

/**
 * Moves between source listings without discarding the applied search.
 *
 * Leaving a search for Popular or Latest parks it so the filter chip can return to it in one tap; the draft editor
 * is left untouched so nothing the user configured is lost.
 */
internal fun CatalogScreenModel.State.switchListing(target: CatalogScreenModel.Listing): CatalogScreenModel.State {
    val parked = when {
        target is CatalogScreenModel.Listing.Search -> null
        listing is CatalogScreenModel.Listing.Search -> listing
        else -> parkedSearch
    }
    return copy(
        listing = target,
        parkedSearch = parked,
        toolbarQuery = (target as? CatalogScreenModel.Listing.Search)?.query,
    )
}

/** The parked search the filter chip restores, present only while another listing is shown. */
internal val CatalogScreenModel.State.restorableSearch: CatalogScreenModel.Listing.Search?
    get() = parkedSearch.takeIf { listing !is CatalogScreenModel.Listing.Search }

/**
 * Runs a toolbar search with the filters that are already applied.
 *
 * The draft is only committed through the validated apply action, so an unapplied or invalid draft never reaches
 * the source from here. Outside a search, the source defaults are the applied filters.
 */
internal fun CatalogScreenModel.State.searchWithAppliedFilters(query: String): CatalogScreenModel.State {
    val appliedFilters = (listing as? CatalogScreenModel.Listing.Search)?.filters ?: defaultFilters
    return copy(
        listing = CatalogScreenModel.Listing.Search(query = query, filters = appliedFilters.detachedCopy()),
        parkedSearch = null,
        toolbarQuery = query,
    )
}
