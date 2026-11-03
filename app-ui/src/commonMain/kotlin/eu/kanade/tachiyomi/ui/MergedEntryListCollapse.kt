package eu.kanade.tachiyomi.ui

/**
 * Keeps the first (and therefore newest, for Updates and History) record for each merged entry.
 *
 * A differing actual and visible ID identifies a merged group. Repeated records whose actual and
 * visible IDs match are preserved because they can be distinct children of an unmerged entry.
 */
internal inline fun <T> Iterable<T>.collapseByVisibleEntry(
    actualEntryId: (T) -> Long,
    visibleEntryId: (T) -> Long,
): List<T> {
    val records = toList()
    val mergedEntryIds = records
        .filter { actualEntryId(it) != visibleEntryId(it) }
        .mapTo(mutableSetOf(), visibleEntryId)
    val seen = mutableSetOf<Long>()
    return records.filterTo(mutableListOf()) {
        val visibleId = visibleEntryId(it)
        visibleId !in mergedEntryIds || seen.add(visibleId)
    }
}
