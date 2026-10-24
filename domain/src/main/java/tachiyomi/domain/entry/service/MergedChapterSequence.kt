package tachiyomi.domain.entry.service

import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.library.service.groupedByMergedMember
import tachiyomi.domain.library.service.sortedWithinMergedMembers

/**
 * The members' chapter lists, given in stored merge order, as one list in that order with each member's chapters in
 * [entry]'s sort. The member order stays the stored one, which the defaults below read back from such a list; screens
 * that show a merge sort it for display with its member IDs.
 */
fun Iterable<List<EntryChapter>>.mergedByMember(entry: Entry): List<EntryChapter> {
    val chapterLists = toList()
    return chapterLists.flatten().sortedWithinMergedMembers(
        mergedIds = chapterLists.mapNotNull { it.firstOrNull()?.entryId },
        idSelector = EntryChapter::entryId,
        itemComparator = getChapterSort(entry).let { cmp -> Comparator { a, b -> cmp(a, b) } },
    )
}

/*
 * A merge a user made is shown in the order they arranged it, and when its title sorts newest first it is read from the
 * bottom up. A merge its source supplies can't be rearranged, so it is stored in reading order, oldest first, and shown
 * in the title's sort direction instead; it is read from its first member under either direction.
 */

/** Whether [this] merged entry shows its members in the reverse of their stored order. */
val Entry.showsMergedMembersReversed: Boolean
    get() = mergedBySource && sortDescending()

/** [memberIds], given in stored merge order, in the order [this] merged entry shows them. */
fun Entry.mergedMembersForDisplay(memberIds: List<Long>): List<Long> =
    if (showsMergedMembersReversed) memberIds.asReversed() else memberIds

private fun Entry.mergedMembersForReading(memberIds: List<Long>): List<Long> =
    if (!mergedBySource && sortDescending()) memberIds.asReversed() else memberIds

fun List<EntryChapter>.sortedForMergedDisplay(
    entry: Entry,
    mergedEntryIds: List<Long> = map(EntryChapter::entryId).distinct(),
): List<EntryChapter> {
    return sortedWithinMergedMembers(
        mergedIds = entry.mergedMembersForDisplay(mergedEntryIds),
        idSelector = EntryChapter::entryId,
        itemComparator = getChapterSort(entry).let { cmp -> Comparator { a, b -> cmp(a, b) } },
    )
}

fun List<EntryChapter>.sortedForReading(
    entry: Entry,
    mergedEntryIds: List<Long> = map(EntryChapter::entryId).distinct(),
): List<EntryChapter> {
    return sortedWithinMergedMembers(
        mergedIds = entry.mergedMembersForReading(mergedEntryIds),
        idSelector = EntryChapter::entryId,
        itemComparator = getChapterSort(entry, sortDescending = false).let { cmp -> Comparator { a, b -> cmp(a, b) } },
    )
}

fun List<EntryChapter>.groupedByMergedMember(
    mergedEntryIds: List<Long> = map(EntryChapter::entryId).distinct(),
): List<Pair<Long, List<EntryChapter>>> {
    return groupedByMergedMember(
        mergedIds = mergedEntryIds,
        idSelector = EntryChapter::entryId,
    )
}
