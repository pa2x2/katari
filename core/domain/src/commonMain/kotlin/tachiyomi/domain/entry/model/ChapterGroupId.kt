package tachiyomi.domain.entry.model

/** One section of [entryId]'s chapter list: its [EntryChapterGroup] with [groupKey], or its chapters in no group when null. */
data class ChapterGroupId(
    val entryId: Long,
    val groupKey: String?,
)

val EntryChapter.groupId: ChapterGroupId
    get() = ChapterGroupId(entryId, groupKey)
