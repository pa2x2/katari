package mihon.entry.interactions

import dev.icerock.moko.resources.StringResource
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter

data class EntryChildListRequest(
    val entry: Entry,
    val chapters: List<EntryChapter>,
    val memberIds: List<Long> = chapters.map(EntryChapter::entryId).distinct(),
    val memberTitleById: Map<Long, String> = emptyMap(),
    val fallbackTitle: String = entry.displayTitle,
    val includeMissingCounts: Boolean = true,
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

    data class MissingCount(
        val id: String,
        val count: Int,
    ) : EntryChildListRow

    data class Child(
        val chapter: EntryChapter,
    ) : EntryChildListRow
}
