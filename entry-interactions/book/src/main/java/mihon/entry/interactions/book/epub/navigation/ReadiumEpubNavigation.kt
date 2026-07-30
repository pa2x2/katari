package mihon.entry.interactions.book.epub

import mihon.book.api.BookLocator
import mihon.book.api.BookNavigationItem
import mihon.book.api.BookReadingDirection
import org.readium.r2.navigator.preferences.ReadingProgression
import org.readium.r2.shared.publication.Locator

internal data class ReadiumNavigationRow(
    val item: BookNavigationItem,
    val depth: Int,
)

internal data class ReadiumPaginatedSectionMetrics(
    val index: Int,
    val startPageIndex: Int,
    val endPageIndex: Int,
)

internal data class ReadiumNavigationPosition(
    val progression: Double,
    val pageIndex: Int?,
)

internal fun Locator.progressionOnly(progression: Double): Locator = Locator(
    href = href.removeFragment(),
    mediaType = mediaType,
    title = title,
    locations = Locator.Locations(progression = progression.coerceIn(0.0, 1.0)),
)

internal fun ReadingProgression.toBookReadingDirection(): BookReadingDirection = when (this) {
    ReadingProgression.LTR -> BookReadingDirection.LEFT_TO_RIGHT
    ReadingProgression.RTL -> BookReadingDirection.RIGHT_TO_LEFT
}

internal fun physicalPageIndexToLogical(
    pageIndex: Int,
    totalPages: Int,
    readingDirection: BookReadingDirection,
): Int {
    val safeTotal = totalPages.coerceAtLeast(1)
    val physical = pageIndex.coerceIn(0, safeTotal - 1)
    return if (readingDirection == BookReadingDirection.RIGHT_TO_LEFT) {
        safeTotal - physical - 1
    } else {
        physical
    }
}

internal fun List<BookNavigationItem>.flattenNavigation(depth: Int = 0): List<ReadiumNavigationRow> =
    flatMap { item ->
        listOf(ReadiumNavigationRow(item, depth)) + item.children.flattenNavigation(depth + 1)
    }

internal fun resolvePaginatedSectionMetrics(
    navigation: List<ReadiumNavigationRow>,
    locator: BookLocator,
    resolvedPositions: Map<String, ReadiumNavigationPosition>,
    currentPageIndex: Int,
    totalPages: Int,
    preferredIndex: Int = -1,
): ReadiumPaginatedSectionMetrics? {
    val candidates = navigation.mapIndexedNotNull { index, row ->
        if (row.item.target.resourceId != locator.resourceId) return@mapIndexedNotNull null
        val position = row.item.target.resolvedNavigationPosition(resolvedPositions) ?: return@mapIndexedNotNull null
        val pageIndex = (
            position.pageIndex
                ?: (position.progression * totalPages).toInt()
            ).coerceIn(0, totalPages - 1)
        NavigationCandidate(index, position.progression, pageIndex)
    }
    if (candidates.isEmpty()) return null

    val current = candidates
        .filter { it.pageIndex <= currentPageIndex }
        .maxWithOrNull(compareBy<NavigationCandidate> { it.pageIndex }.thenBy { it.index })
        ?: candidates.first()
    val selected = candidates.firstOrNull { it.index == preferredIndex }
        ?.takeIf { preferred ->
            preferred.pageIndex <= currentPageIndex &&
                candidates.none {
                    it.index > preferredIndex &&
                        it.pageIndex > preferred.pageIndex &&
                        it.pageIndex <= currentPageIndex
                }
        }
        ?: current
    val endPageIndex = candidates
        .asSequence()
        .filter { it.index > selected.index && it.pageIndex > selected.pageIndex }
        .minByOrNull { it.pageIndex }
        ?.pageIndex
        ?: totalPages

    return ReadiumPaginatedSectionMetrics(
        index = selected.index,
        startPageIndex = selected.pageIndex.coerceIn(0, totalPages - 1),
        endPageIndex = endPageIndex.coerceIn(selected.pageIndex + 1, totalPages),
    )
}

internal fun BookLocator.navigationKey(): String = buildString {
    append(resourceId)
    append('\u0000')
    append(fragments.firstOrNull().orEmpty())
}

internal fun BookLocator.resolvedNavigationPosition(
    resolvedPositions: Map<String, ReadiumNavigationPosition>,
): ReadiumNavigationPosition? = resolvedPositions[navigationKey()]
    ?: progression?.let { ReadiumNavigationPosition(it, null) }
    ?: ReadiumNavigationPosition(0.0, 0).takeIf { fragments.isEmpty() }

private data class NavigationCandidate(
    val index: Int,
    val progression: Double,
    val pageIndex: Int,
)
