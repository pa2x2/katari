package mihon.entry.interactions.child

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.model.EntryChapterGroup

class ChapterGroupRowsTest {

    @Test
    fun `a volume whose chapters are all filtered out has no header while an announced volume has one`() {
        val entry = Entry.create().copy(
            id = 1L,
            chapterFlags = Entry.CHAPTER_SORT_DESC or Entry.CHAPTER_SORTING_NUMBER,
        )
        val groups = listOf(
            EntryChapterGroup(entryId = 1L, key = "v1", title = "Volume 1", position = 0),
            EntryChapterGroup(entryId = 1L, key = "v2", title = "Volume 2", position = 1),
            EntryChapterGroup(entryId = 1L, key = "v3", title = "Volume 3", position = 2),
        )
        val hiddenByFilter = chapter(id = 11, group = groups[0])
        val shown = listOf(chapter(id = 22, group = groups[1]), chapter(id = 21, group = groups[1]))
        val request = EntryChildListRequest(
            entry = entry,
            chapters = shown,
            chapterGroups = groups,
            allChapters = shown + hiddenByFilter,
        )

        shown.map(EntryChildListRow::Child).withChapterGroupHeaders(request) shouldBe listOf(
            EntryChildListRow.GroupHeader(entryId = 1L, groupKey = "v3", title = "Volume 3"),
            EntryChildListRow.GroupHeader(entryId = 1L, groupKey = "v2", title = "Volume 2"),
            EntryChildListRow.Child(shown[0]),
            EntryChildListRow.Child(shown[1]),
        )
    }

    private fun chapter(id: Long, group: EntryChapterGroup): EntryChapter {
        return EntryChapter.create().copy(
            id = id,
            entryId = 1L,
            url = "/chapter/$id",
            groupKey = group.key,
            groupPosition = group.position,
        )
    }
}
