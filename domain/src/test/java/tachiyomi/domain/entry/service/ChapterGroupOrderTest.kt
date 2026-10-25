package tachiyomi.domain.entry.service

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter

class ChapterGroupOrderTest {

    @Test
    fun `volumes whose numbering restarts are read one after another with the side story published between them`() {
        val entry = Entry.create().copy(
            id = 1L,
            chapterFlags = Entry.CHAPTER_SORT_DESC or Entry.CHAPTER_SORTING_NUMBER,
        )
        // Listed newest first, as sources do
        val chapters = listOf(
            chapter(id = 22, number = 2.0, sourceOrder = 0, group = "v2" to 1),
            chapter(id = 21, number = 1.0, sourceOrder = 1, group = "v2" to 1),
            chapter(id = 90, number = 2.5, sourceOrder = 2, group = null),
            chapter(id = 12, number = 2.0, sourceOrder = 3, group = "v1" to 0),
            chapter(id = 11, number = 1.0, sourceOrder = 4, group = "v1" to 0),
        )

        chapters.sortedForReading(entry).map(EntryChapter::id) shouldBe listOf(11L, 12L, 90L, 21L, 22L)
        chapters.sortedForMergedDisplay(entry).map(EntryChapter::id) shouldBe listOf(22L, 21L, 90L, 12L, 11L)
    }

    private fun chapter(id: Long, number: Double, sourceOrder: Long, group: Pair<String, Int>?): EntryChapter {
        return EntryChapter.create().copy(
            id = id,
            entryId = 1L,
            chapterNumber = number,
            sourceOrder = sourceOrder,
            name = "Chapter $id",
            url = "/chapter/$id",
            groupKey = group?.first,
            groupPosition = group?.second,
        )
    }
}
