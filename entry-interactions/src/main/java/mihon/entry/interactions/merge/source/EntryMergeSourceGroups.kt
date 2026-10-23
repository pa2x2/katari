package mihon.entry.interactions.merge.source

import mihon.entry.interactions.merge.host.EntryMergeMembershipSnapshot
import mihon.entry.interactions.merge.host.EntryMergeProfileHost
import tachiyomi.domain.entry.model.Entry

/**
 * The merge of an entry merged by its source. The root and every member its source listed last are locked; the
 * other members were added by a user.
 */
internal data class EntryMergeSourceGroup(
    val root: Entry,
    /** The root's merge, or null while the root holds no members. */
    val membership: EntryMergeMembershipSnapshot?,
    val listing: List<Long>,
) {
    val memberIds: List<Long> = membership?.orderedEntryIds ?: listOf(root.id)

    val lockedIds: Set<Long> = memberIds.filterTo(mutableSetOf()) { it == root.id || it in listing }
}

/** The source group [entry] belongs to, as its root or as a member of the root's merge; null for any other entry. */
internal suspend fun EntryMergeProfileHost.sourceGroupOf(
    entry: Entry,
    membership: EntryMergeMembershipSnapshot?,
): EntryMergeSourceGroup? {
    val root = when {
        entry.mergedBySource -> entry
        membership == null -> return null
        else -> entries(listOf(membership.targetEntryId)).singleOrNull()?.takeIf(Entry::mergedBySource) ?: return null
    }
    return EntryMergeSourceGroup(
        root = root,
        membership = membership?.takeIf { it.targetEntryId == root.id },
        listing = if (root.id > 0L) sourceListing(root.id) else emptyList(),
    )
}

/** Whether [entryId] is a locked member of [membership], so a newer source listing may take it. */
internal suspend fun EntryMergeProfileHost.isLockedIn(
    membership: EntryMergeMembershipSnapshot,
    entryId: Long,
): Boolean {
    val target = entries(listOf(membership.targetEntryId)).singleOrNull() ?: return false
    return target.mergedBySource && entryId in sourceListing(target.id)
}

/**
 * The order of a source group's merge after its source listed [listed]: the root first, members a user added where
 * they were, [unlinked] members gone, and the [claimed] members in source order in the places locked members hold. A
 * newly listed member goes right after the listed member before it.
 */
internal fun sourceGroupOrder(
    current: List<Long>,
    rootId: Long,
    listed: List<Long>,
    claimed: Set<Long>,
    unlinked: Set<Long>,
): List<Long> {
    val lockedInOrder = listed.filter(claimed::contains)
    val order = current.filterTo(mutableListOf()) { it != rootId && it !in unlinked }
    order.add(0, rootId)
    lockedInOrder.forEachIndexed { index, entryId ->
        if (entryId in order) return@forEachIndexed
        val previous = lockedInOrder.subList(0, index).lastOrNull(order::contains)
        order.add(if (previous == null) 1 else order.indexOf(previous) + 1, entryId)
    }
    order.indices.filter {
        order[it] in claimed
    }.zip(lockedInOrder).forEach { (slot, entryId) -> order[slot] = entryId }
    return order
}
