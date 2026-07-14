package mihon.entry.interactions.book.epub

import mihon.book.api.BookLocator
import mihon.book.api.BookNavigationItem

internal data class ReadiumNavigationRow(
    val item: BookNavigationItem,
    val depth: Int,
)

internal data class ReadiumSectionMetrics(
    val index: Int,
    val startProgression: Double,
    val endProgression: Double,
)

internal data class ReadiumPaginatedSectionMetrics(
    val index: Int,
    val startProgression: Double,
    val endProgression: Double,
    val startPageIndex: Int,
    val endPageIndex: Int,
)

internal data class ReadiumNavigationPosition(
    val progression: Double,
    val pageIndex: Int?,
)

internal fun List<BookNavigationItem>.flattenNavigation(depth: Int = 0): List<ReadiumNavigationRow> =
    flatMap { item ->
        listOf(ReadiumNavigationRow(item, depth)) + item.children.flattenNavigation(depth + 1)
    }

internal fun resolveSectionMetrics(
    navigation: List<ReadiumNavigationRow>,
    locator: BookLocator,
    resolvedPositions: Map<String, ReadiumNavigationPosition>,
    preferredIndex: Int = -1,
): ReadiumSectionMetrics? {
    val progression = locator.progression ?: 0.0
    val candidates = navigation.mapIndexedNotNull { index, row ->
        if (row.item.target.resourceId != locator.resourceId) return@mapIndexedNotNull null
        row.item.target.resolvedNavigationPosition(resolvedPositions)?.progression?.let { index to it }
    }
    if (candidates.isEmpty()) return null

    val current = candidates
        .filter { (_, start) -> start <= progression + PROGRESSION_EPSILON }
        .maxWithOrNull(compareBy<Pair<Int, Double>> { it.second }.thenBy { it.first })
        ?: candidates.first()
    val selected = candidates.firstOrNull { it.first == preferredIndex }
        ?.takeIf { (_, start) ->
            progression + PROGRESSION_EPSILON >= start &&
                candidates.none { (index, candidateStart) ->
                    index > preferredIndex &&
                        candidateStart > start &&
                        candidateStart <= progression + PROGRESSION_EPSILON
                }
        }
        ?: current
    val end = candidates
        .asSequence()
        .filter { (index, start) -> index > selected.first && start > selected.second + PROGRESSION_EPSILON }
        .minByOrNull { it.second }
        ?.second
        ?: 1.0

    return ReadiumSectionMetrics(
        index = selected.first,
        startProgression = selected.second.coerceIn(0.0, 1.0),
        endProgression = end.coerceIn(selected.second, 1.0),
    )
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
    val next = candidates
        .asSequence()
        .filter { it.index > selected.index && it.progression > selected.progression + PROGRESSION_EPSILON }
        .minByOrNull { it.progression }
    val endPageIndex = candidates
        .asSequence()
        .filter { it.index > selected.index && it.pageIndex > selected.pageIndex }
        .minByOrNull { it.pageIndex }
        ?.pageIndex
        ?: totalPages

    return ReadiumPaginatedSectionMetrics(
        index = selected.index,
        startProgression = selected.progression.coerceIn(0.0, 1.0),
        endProgression = (next?.progression ?: 1.0).coerceIn(selected.progression, 1.0),
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

private const val PROGRESSION_EPSILON = 0.0001
