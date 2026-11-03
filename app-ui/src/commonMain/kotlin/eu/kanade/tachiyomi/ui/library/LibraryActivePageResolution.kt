package eu.kanade.tachiyomi.ui.library

import tachiyomi.domain.library.model.LibraryGrouping
import tachiyomi.domain.library.model.LibraryGroupingDimension

/**
 * Picks the page the library shows after its pages are regrouped (grouping, filters, search).
 *
 * The page the user chose wins whenever it exists. When a filter or a regrouping hides it, the library falls back to
 * the page matching [previousPage] on the dimensions both groupings share, or to the first page. The fallback is
 * temporary: the caller keeps [chosenPageId] as it was, so the chosen page comes back once it reappears.
 *
 * [previousPage] is null on the first grouping after a load; [fallbackPageIndex] is then the persisted index of the
 * choice, used when its page is missing.
 */
internal fun List<LibraryPage>.resolveActivePageIndex(
    chosenPageId: String,
    previousPage: LibraryPage?,
    fallbackPageIndex: Int,
    previousGrouping: LibraryGrouping,
    newGrouping: LibraryGrouping,
): Int {
    if (isEmpty()) return fallbackPageIndex
    val chosenIndex = indexOfFirst { it.id == chosenPageId }
    if (chosenIndex >= 0) return chosenIndex
    if (previousPage == null) return fallbackPageIndex.coerceIn(0, lastIndex)

    val sharedDimensions = previousGrouping.dimensions.intersect(newGrouping.dimensions.toSet())
    return indexOfFirst { candidate ->
        sharedDimensions.all { dimension ->
            when (dimension) {
                LibraryGroupingDimension.Category ->
                    previousPage.category?.id?.let { candidate.category?.id == it } ?: true
                LibraryGroupingDimension.EntryType ->
                    previousPage.entryType?.let { candidate.entryType == it } ?: true
                LibraryGroupingDimension.Source ->
                    previousPage.sourceId?.let { candidate.sourceId == it } ?: true
            }
        }
    }.takeIf { it >= 0 } ?: 0
}
