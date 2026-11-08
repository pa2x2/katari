package tachiyomi.domain.util

internal fun <T> List<Long>.orderedPresentIds(items: List<T>, idOf: (T) -> Long): List<Long> {
    if (isEmpty()) return items.map(idOf).distinct()

    val presentIds = items.map(idOf).toSet()
    return asSequence()
        .filter { it in presentIds }
        .toList()
}
