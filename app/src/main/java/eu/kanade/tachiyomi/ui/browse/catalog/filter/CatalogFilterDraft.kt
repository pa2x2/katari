package eu.kanade.tachiyomi.ui.browse.catalog

import eu.kanade.domain.source.model.FeedListingMode
import eu.kanade.domain.source.model.FilterStateNode
import eu.kanade.domain.source.model.restoreSnapshot
import eu.kanade.domain.source.model.snapshot
import eu.kanade.tachiyomi.source.entry.EntryFilterList
import eu.kanade.tachiyomi.source.entry.filter.validationIssues
import eu.kanade.tachiyomi.source.filter.detachedCopy

internal fun CatalogViewModel.State.initializeForSource(
    sourceFilters: EntryFilterList,
    initialFilterSnapshot: List<FilterStateNode> = emptyList(),
): CatalogViewModel.State {
    val defaults = sourceFilters.detachedCopy()
    val filters = sourceFilters
    val restoration = filters.restoreSnapshot(initialFilterSnapshot)
    val query = (listing as? CatalogViewModel.Listing.Search)?.query
    val updatedListing = when (listing) {
        is CatalogViewModel.Listing.Search -> CatalogViewModel.Listing.Search(query, filters.detachedCopy())
        else -> listing
    }

    return withRepublishedDraftFilters(filters).copy(
        filterState = FilterUiState.Ready,
        filterResetPending = false,
        listing = updatedListing,
        defaultFilters = defaults,
        appliedFiltersReady = restoration.isCompatible && filters.validationIssues().isEmpty(),
        repairIssues = restoration.issues,
        repairNeedsSave = initialFilterSnapshot.isNotEmpty() &&
            (restoration.issues.isNotEmpty() || filters.validationIssues().isNotEmpty()),
        toolbarQuery = query,
    )
}

internal data class SavedPresetState(
    val listingMode: FeedListingMode,
    val query: String?,
    val filters: List<FilterStateNode>,
)

internal fun CatalogViewModel.State.toSavedPresetState(defaultFilters: EntryFilterList): SavedPresetState {
    val filterSnapshot = filters.snapshot()
    val hasEditedFilters = filterSnapshot != defaultFilters.snapshot()
    val listingMode = when {
        hasEditedFilters || draftMode == FeedListingMode.Search ||
            (draftMode == null && listing is CatalogViewModel.Listing.Search) -> FeedListingMode.Search
        draftMode == FeedListingMode.Popular ||
            (draftMode == null && listing == CatalogViewModel.Listing.Popular) -> FeedListingMode.Popular
        else -> FeedListingMode.Latest
    }
    val query = draftSearchQuery
        ?.trim()
        ?.takeIf { listingMode == FeedListingMode.Search && it.isNotEmpty() }

    return SavedPresetState(
        listingMode = listingMode,
        query = query,
        filters = filterSnapshot,
    )
}

internal val CatalogViewModel.State.hasUnappliedFilterChanges: Boolean
    get() {
        val draft = toSavedPresetState(defaultFilters)
        val applied = when (val current = listing) {
            CatalogViewModel.Listing.Popular -> SavedPresetState(
                FeedListingMode.Popular,
                null,
                defaultFilters.snapshot(),
            )
            CatalogViewModel.Listing.Latest -> SavedPresetState(
                FeedListingMode.Latest,
                null,
                defaultFilters.snapshot(),
            )
            is CatalogViewModel.Listing.Search -> SavedPresetState(
                FeedListingMode.Search,
                current.query?.trim()?.ifEmpty {
                    null
                },
                current.filters.snapshot(),
            )
        }
        return draft != applied
    }

/** A validated editor commit is the only filter-sheet action that replaces the result listing. */
internal fun CatalogViewModel.State.applyFilterDraft(): CatalogViewModel.State? {
    if (!canSaveFilterDraft || repairNeedsSave) return null
    val saved = toSavedPresetState(defaultFilters)
    val applied = when (saved.listingMode) {
        FeedListingMode.Popular -> CatalogViewModel.Listing.Popular
        FeedListingMode.Latest -> CatalogViewModel.Listing.Latest
        FeedListingMode.Search -> CatalogViewModel.Listing.Search(saved.query, filters.detachedCopy())
    }
    return copy(
        listing = applied,
        parkedSearch = null,
        toolbarQuery = saved.query,
        draftMode = null,
        draftQuery = null,
        appliedCustomPresetId = draftPresetId,
        appliedFiltersReady = true,
    )
}

/** Draft loading and validation cannot suspend an already applied search. */
internal val CatalogViewModel.State.pageableListing: CatalogViewModel.Listing?
    get() = listing.takeUnless { it is CatalogViewModel.Listing.Search && !appliedFiltersReady }
