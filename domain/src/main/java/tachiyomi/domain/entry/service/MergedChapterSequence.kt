package tachiyomi.domain.entry.service

import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.library.service.groupedByMergedMember
import tachiyomi.domain.library.service.sortedForMergedDisplay
import tachiyomi.domain.library.service.sortedForReading

fun List<EntryChapter>.sortedForMergedDisplay(
    entry: Entry,
    mergedEntryIds: List<Long> = map(EntryChapter::entryId).distinct(),
): List<EntryChapter> {
    return sortedForMergedDisplay(
        mergedIds = mergedEntryIds,
        idSelector = EntryChapter::entryId,
        itemComparator = getChapterSort(entry).let { cmp -> Comparator { a, b -> cmp(a, b) } },
    )
}

fun List<EntryChapter>.sortedForReading(
    entry: Entry,
    mergedEntryIds: List<Long> = map(EntryChapter::entryId).distinct(),
): List<EntryChapter> {
    return sortedForReading(
        mergedIds = mergedEntryIds,
        idSelector = EntryChapter::entryId,
        itemComparator = getChapterSort(entry, sortDescending = false).let { cmp -> Comparator { a, b -> cmp(a, b) } },
        sortDescending = entry.sortDescending(),
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
