package eu.kanade.tachiyomi.source.entry

/** Result of [UnifiedSource.refresh]. */
class EntryRefresh(
    val entry: SEntry,
    val chapters: List<SEntryChapter>,
    /**
     * The groups [chapters] refer to, in reading order. Read only when chapters were fetched; empty when the entry's
     * chapters aren't grouped.
     */
    val chapterGroups: List<SEntryChapterGroup> = emptyList(),
)
