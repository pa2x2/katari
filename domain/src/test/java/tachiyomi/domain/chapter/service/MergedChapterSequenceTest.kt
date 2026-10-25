package tachiyomi.domain.chapter.service

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.service.mergedByMember
import tachiyomi.domain.entry.service.sortedForMergedDisplay
import tachiyomi.domain.entry.service.sortedForReading

class MergedChapterSequenceTest {

    @Test
    fun `merged display follows member order while reading follows descending group traversal`() {
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
        chapters.sortedForReading(manga).map(EntryChapter::id) shouldBe listOf(201L, 202L, 203L, 101L)
    }

    @Test
    fun `a merge its source supplies is listed in the sort direction and read from its first member either way`() {
        val seasons = Entry.create().copy(id = 1L, mergedBySource = true)
        val newestFirst = seasons.copy(chapterFlags = Entry.CHAPTER_SORT_DESC or Entry.CHAPTER_SORTING_NUMBER)
        val oldestFirst = seasons.copy(chapterFlags = Entry.CHAPTER_SORT_ASC or Entry.CHAPTER_SORTING_NUMBER)
        val seasonOne = listOf(
            chapter(id = 201, entryId = 2, chapterNumber = 1.0),
            chapter(id = 202, entryId = 2, chapterNumber = 2.0),
        )
        val seasonTwo = listOf(
            chapter(id = 301, entryId = 3, chapterNumber = 1.0),
            chapter(id = 302, entryId = 3, chapterNumber = 2.0),
        )
        val chapters = seasonOne + seasonTwo
        val seasonOneFirst = listOf(1L, 2L, 3L)

        chapters.sortedForMergedDisplay(newestFirst, seasonOneFirst).map(EntryChapter::id) shouldBe
            listOf(302L, 301L, 202L, 201L)
        chapters.sortedForMergedDisplay(oldestFirst, seasonOneFirst).map(EntryChapter::id) shouldBe
            listOf(201L, 202L, 301L, 302L)
        // Continue reads the member order back from the fetched list
        listOf(seasonOne, seasonTwo).mergedByMember(newestFirst).sortedForReading(newestFirst)
            .map(EntryChapter::id) shouldBe listOf(201L, 202L, 301L, 302L)
        listOf(seasonOne, seasonTwo).mergedByMember(oldestFirst).sortedForReading(oldestFirst)
            .map(EntryChapter::id) shouldBe listOf(201L, 202L, 301L, 302L)
    }

    @Test
    fun `merged orders ignore removed member ids that no longer have chapters`() {
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
