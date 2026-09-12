package eu.kanade.tachiyomi.ui.browse.catalog

import eu.kanade.tachiyomi.source.entry.EntryFilterList

/**
 * Republishes the mutable draft list with a new monotonic revision.
 *
 * Draft entries mutate state in place and [EntryFilterList] never equals another list, but paged
 * sheets are hosted in `AnimatedContent` keyed on a stable navigation path. When an edit mutates
 * the same filter instance and the apply gate returns to the same value, every composable param
 * compares equal and the paged content is skipped. Bumping [CatalogScreenModel.State.filterRevision]
 * on every republish gives paged content a stable, differing param that forces recomposition
 * without restarting the navigation transition or resetting paging and scroll state.
 */
internal fun CatalogScreenModel.State.withRepublishedDraftFilters(
    filters: EntryFilterList,
): CatalogScreenModel.State = copy(filters = filters, filterRevision = filterRevision + 1)
