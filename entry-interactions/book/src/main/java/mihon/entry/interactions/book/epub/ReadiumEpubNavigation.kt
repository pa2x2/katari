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

internal fun List<BookNavigationItem>.flattenNavigation(depth: Int = 0): List<ReadiumNavigationRow> =
    flatMap { item ->
        listOf(ReadiumNavigationRow(item, depth)) + item.children.flattenNavigation(depth + 1)
    }

internal fun resolveSectionMetrics(
    navigation: List<ReadiumNavigationRow>,
    locator: BookLocator,
    resolvedProgressions: Map<String, Double>,
    preferredIndex: Int = -1,
): ReadiumSectionMetrics? {
    val progression = locator.progression ?: 0.0
    val candidates = navigation.mapIndexedNotNull { index, row ->
        if (row.item.target.resourceId != locator.resourceId) return@mapIndexedNotNull null
        row.item.target.resolvedNavigationProgression(resolvedProgressions)?.let { index to it }
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

internal fun BookLocator.navigationKey(): String = buildString {
    append(resourceId)
    append('\u0000')
    append(fragments.firstOrNull().orEmpty())
}

internal fun BookLocator.resolvedNavigationProgression(resolvedProgressions: Map<String, Double>): Double? =
    progression
        ?: resolvedProgressions[navigationKey()]
        ?: 0.0.takeIf { fragments.isEmpty() }

private const val PROGRESSION_EPSILON = 0.0001
