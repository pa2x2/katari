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
    /**
     * What follows [chapters]: [ChapterListRest.Complete] when they are the whole list. Only a
     * [ChapterListPartsSource] may answer [ChapterListRest.More]; from any other source it counts as
     * [ChapterListRest.Unchanged].
     */
    val rest: ChapterListRest = ChapterListRest.Complete,
)
