package tachiyomi.domain.chapter.service

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.service.sortedForMergedDisplay
import tachiyomi.domain.entry.service.sortedForReading

class MergedChapterSequenceTest {

    @Test
    fun `merged display order follows member order and visible descending sort`() {
        val manga = Entry.create().copy(
            id = 1L,
            chapterFlags = Entry.CHAPTER_SORT_DESC or Entry.CHAPTER_SORTING_NUMBER,
        )

        val chapters = listOf(
            chapter(id = 101, entryId = 1, chapterNumber = 1.0),
            chapter(id = 203, entryId = 2, chapterNumber = 3.0),
            chapter(id = 202, entryId = 2, chapterNumber = 2.0),
            chapter(id = 201, entryId = 2, chapterNumber = 1.0),
        )

        chapters.sortedForMergedDisplay(manga).map(EntryChapter::id) shouldBe listOf(101L, 203L, 202L, 201L)
    }

    @Test
    fun `merged reading order follows descending group traversal and canonical ascending chapters`() {
        val manga = Entry.create().copy(
            id = 1L,
            chapterFlags = Entry.CHAPTER_SORT_DESC or Entry.CHAPTER_SORTING_NUMBER,
        )

        val chapters = listOf(
            chapter(id = 101, entryId = 1, chapterNumber = 1.0),
            chapter(id = 203, entryId = 2, chapterNumber = 3.0),
            chapter(id = 202, entryId = 2, chapterNumber = 2.0),
            chapter(id = 201, entryId = 2, chapterNumber = 1.0),
        )

        chapters.sortedForReading(manga).map(EntryChapter::id) shouldBe listOf(201L, 202L, 203L, 101L)
    }

    @Test
    fun `merged reading order keeps top to bottom groups when sort is ascending`() {
        val manga = Entry.create().copy(
            id = 1L,
            chapterFlags = Entry.CHAPTER_SORT_ASC or Entry.CHAPTER_SORTING_NUMBER,
        )

        val chapters = listOf(
            chapter(id = 101, entryId = 1, chapterNumber = 1.0),
            chapter(id = 203, entryId = 2, chapterNumber = 3.0),
            chapter(id = 202, entryId = 2, chapterNumber = 2.0),
            chapter(id = 201, entryId = 2, chapterNumber = 1.0),
        )

        chapters.sortedForReading(manga).map(EntryChapter::id) shouldBe listOf(101L, 201L, 202L, 203L)
    }

    @Test
    fun `non merged chapters keep reader ascending order`() {
        val manga = Entry.create().copy(
            id = 1L,
            chapterFlags = Entry.CHAPTER_SORT_DESC or Entry.CHAPTER_SORTING_NUMBER,
        )

        val chapters = listOf(
            chapter(id = 103, entryId = 1, chapterNumber = 3.0),
            chapter(id = 102, entryId = 1, chapterNumber = 2.0),
            chapter(id = 101, entryId = 1, chapterNumber = 1.0),
        )

        chapters.sortedForReading(manga).map(EntryChapter::id) shouldBe listOf(101L, 102L, 103L)
    }

    @Test
    fun `merged display order ignores removed member ids that no longer have chapters`() {
        val manga = Entry.create().copy(
            id = 1L,
            chapterFlags = Entry.CHAPTER_SORT_DESC or Entry.CHAPTER_SORTING_NUMBER,
        )

        val chapters = listOf(
            chapter(id = 101, entryId = 1, chapterNumber = 1.0),
            chapter(id = 301, entryId = 3, chapterNumber = 1.0),
        )

        chapters.sortedForMergedDisplay(manga, mergedEntryIds = listOf(1L, 2L, 3L)).map(EntryChapter::id) shouldBe
            listOf(101L, 301L)
    }

    @Test
    fun `merged reading order ignores removed member ids that no longer have chapters`() {
        val manga = Entry.create().copy(
            id = 1L,
            chapterFlags = Entry.CHAPTER_SORT_DESC or Entry.CHAPTER_SORTING_NUMBER,
        )

        val chapters = listOf(
            chapter(id = 101, entryId = 1, chapterNumber = 1.0),
            chapter(id = 301, entryId = 3, chapterNumber = 1.0),
        )

        chapters.sortedForReading(manga, mergedEntryIds = listOf(1L, 2L, 3L)).map(EntryChapter::id) shouldBe
            listOf(301L, 101L)
    }

    private fun chapter(
        id: Long,
        entryId: Long,
        chapterNumber: Double,
    ): EntryChapter {
        return EntryChapter.create().copy(
            id = id,
            entryId = entryId,
            chapterNumber = chapterNumber,
            name = "Chapter $id",
            url = "/chapter/$id",
        )
    }
}
