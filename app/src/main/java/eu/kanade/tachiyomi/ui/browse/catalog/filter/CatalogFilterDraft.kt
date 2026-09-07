package eu.kanade.tachiyomi.ui.browse.catalog

import eu.kanade.domain.source.model.FeedListingMode
import eu.kanade.domain.source.model.FilterStateNode
import eu.kanade.domain.source.model.restoreSnapshot
import eu.kanade.domain.source.model.snapshot
import eu.kanade.tachiyomi.source.entry.EntryFilterList
import eu.kanade.tachiyomi.source.entry.filter.validationIssues
import eu.kanade.tachiyomi.source.filter.detachedCopy

internal fun CatalogScreenModel.State.initializeForSource(
    sourceFilters: EntryFilterList,
    initialFilterSnapshot: List<FilterStateNode> = emptyList(),
): CatalogScreenModel.State {
    val defaults = sourceFilters.detachedCopy()
    val filters = sourceFilters
    val restoration = filters.restoreSnapshot(initialFilterSnapshot)
    val query = (listing as? CatalogScreenModel.Listing.Search)?.query
    val updatedListing = when (listing) {
        is CatalogScreenModel.Listing.Search -> CatalogScreenModel.Listing.Search(query, filters.detachedCopy())
        else -> listing
    }

    return copy(
        filterState = FilterUiState.Ready,
        filterResetPending = false,
        listing = updatedListing,
        filters = filters,
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

internal fun CatalogScreenModel.State.toSavedPresetState(defaultFilters: EntryFilterList): SavedPresetState {
    val filterSnapshot = filters.snapshot()
    val hasEditedFilters = filterSnapshot != defaultFilters.snapshot()
    val listingMode = when {
        hasEditedFilters || draftMode == FeedListingMode.Search ||
            (draftMode == null && listing is CatalogScreenModel.Listing.Search) -> FeedListingMode.Search
        draftMode == FeedListingMode.Popular ||
            (draftMode == null && listing == CatalogScreenModel.Listing.Popular) -> FeedListingMode.Popular
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

internal val CatalogScreenModel.State.hasUnappliedFilterChanges: Boolean
    get() {
        val draft = toSavedPresetState(defaultFilters)
        val applied = when (val current = listing) {
            CatalogScreenModel.Listing.Popular -> SavedPresetState(
                FeedListingMode.Popular,
                null,
                defaultFilters.snapshot(),
            )
            CatalogScreenModel.Listing.Latest -> SavedPresetState(
                FeedListingMode.Latest,
                null,
                defaultFilters.snapshot(),
            )
            is CatalogScreenModel.Listing.Search -> SavedPresetState(
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
internal fun CatalogScreenModel.State.applyFilterDraft(): CatalogScreenModel.State? {
    if (!canSaveFilterDraft || repairNeedsSave) return null
    val saved = toSavedPresetState(defaultFilters)
    val applied = when (saved.listingMode) {
        FeedListingMode.Popular -> CatalogScreenModel.Listing.Popular
        FeedListingMode.Latest -> CatalogScreenModel.Listing.Latest
        FeedListingMode.Search -> CatalogScreenModel.Listing.Search(saved.query, filters.detachedCopy())
    }
    return copy(
        listing = applied,
        toolbarQuery = saved.query,
        draftMode = null,
        draftQuery = null,
        appliedCustomPresetId = draftPresetId,
        appliedFiltersReady = true,
    )
}

/** Draft loading and validation cannot suspend an already applied search. */
internal val CatalogScreenModel.State.pageableListing: CatalogScreenModel.Listing?
    get() = listing.takeUnless { it is CatalogScreenModel.Listing.Search && !appliedFiltersReady }
