package tachiyomi.domain.entry.service

import tachiyomi.domain.entry.model.EntryChapterGroup

/**
 * Merges the groups one part of a chapter list declares into the entry's stored groups, for a list that arrives in
 * parts and so never declares all its groups at once.
 *
 * [declared] are the part's groups in reading order. A declared group the entry already has keeps its place and takes
 * the declared title; new groups go between the known groups they are declared between. New groups with no known group
 * declared around them go just before [unanchoredBefore], the group of the nearest newer chapter already saved, or at
 * the end when there is none, as the newest part's groups do.
 *
 * Stored groups keep their positions whenever the new ones fit between them, so chapters, which copy their group's
 * position, rarely need rewriting; only when they don't fit is every group renumbered.
 */
fun mergeChapterGroups(
    entryId: Long,
    stored: List<EntryChapterGroup>,
    declared: List<Pair<String, String>>,
    unanchoredBefore: String?,
): List<EntryChapterGroup> {
    val titles = stored.associate { it.key to it.title } + declared
    val positions = stored.associate { it.key to it.position }
    val keys = stored.sortedBy(EntryChapterGroup::position).map(EntryChapterGroup::key).toMutableList()
    val pending = mutableListOf<String>()
    var lastAnchor: String? = null
    declared.map(Pair<String, String>::first).distinct().forEach { key ->
        if (key in positions) {
            if (pending.isNotEmpty()) {
                keys.addAll(keys.indexOf(key), pending)
                pending.clear()
            }
            lastAnchor = key
        } else {
            pending += key
        }
    }
    if (pending.isNotEmpty()) {
        val index = when (val anchor = lastAnchor) {
            null -> unanchoredBefore?.let(keys::indexOf)?.takeIf { it >= 0 } ?: keys.size
            else -> keys.indexOf(anchor) + 1
        }
        keys.addAll(index, pending)
    }
    val merged = assignPositions(keys, positions)
    return keys.map { key ->
        EntryChapterGroup(entryId = entryId, key = key, title = titles.getValue(key), position = merged.getValue(key))
    }
}

// Gives the new keys positions between their stored neighbours, renumbering everything only when they don't fit
private fun assignPositions(keys: List<String>, stored: Map<String, Int>): Map<String, Int> {
    val result = mutableMapOf<String, Int>()
    var index = 0
    while (index < keys.size) {
        val key = keys[index]
        val position = stored[key]
        if (position != null) {
            result[key] = position
            index++
            continue
        }
        val runEnd = (index until keys.size).firstOrNull { keys[it] in stored } ?: keys.size
        val run = keys.subList(index, runEnd)
        val before = keys.getOrNull(index - 1)?.let(result::get)
        val after = keys.getOrNull(runEnd)?.let(stored::get)
        val start = when {
            before == null && after == null -> 0
            before == null -> after!! - run.size
            after == null || after - before - 1 >= run.size -> before + 1
            else -> return keys.withIndex().associate { (position, it) -> it to position }
        }
        run.forEachIndexed { offset, newKey -> result[newKey] = start + offset }
        index = runEnd
    }
    return result
}
