package eu.kanade.tachiyomi.util.chapter

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.interactor.GetNextUnreadChapter
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.repository.EntryChapterRepository

class ChapterGetNextUnreadTest {

    @Test
    fun `returns unread chapter with lowest source order`() = runTest {
        val repository = mockk<EntryChapterRepository>()
        val chapters = listOf(
            chapter(id = 105, sourceOrder = 2, read = false),
            chapter(id = 104, sourceOrder = 1, read = false),
            chapter(id = 103, sourceOrder = 0, read = true),
        )
        coEvery { repository.getChaptersByEntryIdAwait(1L) } returns chapters

        GetNextUnreadChapter(repository).await(1L)?.id shouldBe 104L
    }

    @Test
    fun `returns next unread chapter after current chapter`() = runTest {
        val repository = mockk<EntryChapterRepository>()
        val chapters = listOf(
            chapter(id = 101, sourceOrder = 0, chapterNumber = 1.0, read = false),
            chapter(id = 102, sourceOrder = 1, chapterNumber = 2.0, read = true),
            chapter(id = 103, sourceOrder = 2, chapterNumber = 3.0, read = false),
        )
        coEvery { repository.getChaptersByEntryIdAwait(1L) } returns chapters

        GetNextUnreadChapter(repository).await(1L, chapters.first())?.id shouldBe 103L
    }

    @Test
    fun `returns null when no unread chapter follows current chapter`() = runTest {
        val repository = mockk<EntryChapterRepository>()
        val chapters = listOf(
            chapter(id = 101, sourceOrder = 0, chapterNumber = 1.0, read = false),
            chapter(id = 102, sourceOrder = 1, chapterNumber = 2.0, read = true),
        )
        coEvery { repository.getChaptersByEntryIdAwait(1L) } returns chapters

        GetNextUnreadChapter(repository).await(1L, chapters.first()).shouldBeNull()
    }

    private fun chapter(
        id: Long,
        sourceOrder: Long,
        chapterNumber: Double = sourceOrder.toDouble(),
        read: Boolean,
    ): EntryChapter {
        return EntryChapter.create().copy(
            id = id,
            entryId = 1L,
            sourceOrder = sourceOrder,
            chapterNumber = chapterNumber,
            read = read,
            name = "Chapter $id",
            url = "/chapter/$id",
        )
    }
}
