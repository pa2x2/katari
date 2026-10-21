package eu.kanade.tachiyomi.ui.download

import mihon.entry.interactions.download.EntryDownloadQueueGroup
import mihon.entry.interactions.download.EntryDownloadQueueItem

/*
 * The queue is shown in source groups, and a download never leaves its group: edits reorder items within a group or
 * whole groups, then submit the flattened queue.
 */

internal fun <R : Comparable<R>> List<EntryDownloadQueueGroup>.sortedWithinGroups(
    selector: (EntryDownloadQueueItem) -> R,
    reverse: Boolean,
): List<EntryDownloadQueueItem> = flatMap { group ->
    val sorted = group.items.sortedBy(selector)
    if (reverse) sorted.asReversed() else sorted
}

internal fun List<EntryDownloadQueueGroup>.withItemMoved(
    item: EntryDownloadQueueItem,
    moveToTop: Boolean,
): List<EntryDownloadQueueItem> = flatMap { group ->
    if (group.items.none { it.identity == item.identity }) return@flatMap group.items
    val others = group.items.filterNot { it.identity == item.identity }
    if (moveToTop) listOf(item) + others else others + item
}

internal val EntryDownloadQueueGroup.key: String
    get() = "group:${entryType.name}:$sourceId"

internal val EntryDownloadQueueItem.key: String
    get() = "item:${entryType.name}:$childId"

/** The keys of the groups and of each group's items, as last arranged by a drag. */
internal data class DownloadQueueOrder(val groups: List<String>, val items: Map<String, List<String>>) {
    /**
     * [queue] arranged in this order. The queue keeps changing while a drag settles, so downloads that finished meanwhile
     * drop out and new ones keep their place after the arranged ones.
     */
    fun applyTo(queue: List<EntryDownloadQueueGroup>): List<EntryDownloadQueueGroup> {
        return queue
            .sortedBy { group -> groups.rank(group.key) }
            .map { group ->
                val order = items[group.key] ?: return@map group
                group.copy(items = group.items.sortedBy { item -> order.rank(item.key) })
            }
    }

    /**
     * This order with the group or item keyed [from] moved to where [to] is, or null when they are not two groups or two
     * items of one group.
     */
    fun moved(from: String, to: String): DownloadQueueOrder? {
        if (from in groups && to in groups) return copy(groups = groups.withMoved(from, to))
        val (group, order) = items.entries.firstOrNull { (_, order) -> from in order && to in order } ?: return null
        return copy(items = items + (group to order.withMoved(from, to)))
    }

    /** Whether [queue] already lists its groups and items in this order. */
    fun isMetBy(queue: List<EntryDownloadQueueGroup>): Boolean = applyTo(queue) == queue

    private fun List<String>.withMoved(from: String, to: String): List<String> = toMutableList().apply {
        add(indexOf(to), removeAt(indexOf(from)))
    }

    private fun List<String>.rank(key: String): Int = indexOf(key).takeIf { it >= 0 } ?: Int.MAX_VALUE

    companion object {
        fun of(queue: List<EntryDownloadQueueGroup>) = DownloadQueueOrder(
            groups = queue.map { it.key },
            items = queue.associate { group -> group.key to group.items.map { it.key } },
        )
    }
}
