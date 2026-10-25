package tachiyomi.domain.entry.service

import tachiyomi.domain.entry.model.EntryChapter

/**
 * One entry's chapters that share a group, or that belong to none when [groupKey] is null. The chapter list shows one
 * per group, and every order a list or reader uses runs section by section, so numbering that restarts in each volume
 * never interleaves.
 */
data class ChapterGroupSection(
    val groupKey: String?,
    val chapters: List<EntryChapter>,
)

/**
 * [this] chapters of one entry, or of one merge member, as sections in reading order, each sorted by [comparator].
 * Groups follow their declared position. The ungrouped chapters form one section that goes before the first group whose
 * earliest chapter the source lists after theirs, so chapters published between two volumes stay between them.
 */
fun List<EntryChapter>.groupSectionsInReadingOrder(comparator: Comparator<EntryChapter>): List<ChapterGroupSection> {
    if (isEmpty()) return emptyList()
    if (none { it.groupKey != null }) return listOf(ChapterGroupSection(null, sortedWith(comparator)))

    val (grouped, ungrouped) = partition { it.groupKey != null }
    val sections = grouped
        .groupBy { it.groupKey }
        .values
        .sortedBy { it.first().groupPosition ?: Int.MAX_VALUE }
        .mapTo(mutableListOf()) { ChapterGroupSection(it.first().groupKey, it.sortedWith(comparator)) }
    if (ungrouped.isNotEmpty()) {
        // Sources list newest first, so the earliest chapter has the highest source order
        val ungroupedStart = ungrouped.maxOf(EntryChapter::sourceOrder)
        val before = sections.indexOfFirst { it.chapters.maxOf(EntryChapter::sourceOrder) < ungroupedStart }
        val ungroupedSection = ChapterGroupSection(null, ungrouped.sortedWith(comparator))
        if (before == -1) sections.add(ungroupedSection) else sections.add(before, ungroupedSection)
    }
    return sections
}

/** [this] chapters of one entry sorted by [comparator] within their groups, the groups reversed when [descending]. */
fun List<EntryChapter>.sortedByGroup(comparator: Comparator<EntryChapter>, descending: Boolean): List<EntryChapter> {
    val sections = groupSectionsInReadingOrder(comparator)
    return (if (descending) sections.asReversed() else sections).flatMap(ChapterGroupSection::chapters)
}
