package mihon.entry.interactions.child

import dev.icerock.moko.resources.StringResource
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.model.EntryChapterGroup

data class EntryChildListRequest(
    val entry: Entry,
    val chapters: List<EntryChapter>,
    val memberIds: List<Long> = chapters.map(EntryChapter::entryId).distinct(),
    val memberTitleById: Map<Long, String> = emptyMap(),
    val fallbackTitle: String = entry.displayTitle,
    val includeMissingCounts: Boolean = true,
    /** The groups the members' chapters belong to, each member's in reading order. */
    val chapterGroups: List<EntryChapterGroup> = emptyList(),
    /** Every chapter before the filters, which tells a group with no chapters yet from one whose chapters are hidden. */
    val allChapters: List<EntryChapter> = chapters,
)

data class EntryChildProgressLabel(
    val resource: StringResource,
    val args: List<Any> = emptyList(),
)

data class EntryChildProgressRequest(
    val entry: Entry,
    val chapters: List<EntryChapter>,
    val memberIds: List<Long> = chapters.map(EntryChapter::entryId).distinct(),
)

data class EntryChildListDisplay(
    val rows: List<EntryChildListRow>,
    val aggregateMissingCount: Int,
)

sealed interface EntryChildListRow {
    data class MemberHeader(
        val entryId: Long,
        val title: String,
    ) : EntryChildListRow

    /**
     * Heads one group of a member's chapters, or with a null [groupKey] the chapters in no group. A group whose chapters
     * the filters hide all has none; one followed by no chapters has none yet.
     */
    data class GroupHeader(
        val entryId: Long,
        val groupKey: String?,
        val title: String?,
    ) : EntryChildListRow

    data class MissingCount(
        val id: String,
        val count: Int,
    ) : EntryChildListRow

    data class Child(
        val chapter: EntryChapter,
    ) : EntryChildListRow
}
