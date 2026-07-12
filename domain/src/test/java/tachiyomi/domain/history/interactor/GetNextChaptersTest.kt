package tachiyomi.domain.history.interactor

import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.interactor.GetEntry
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.history.repository.HistoryRepository
import tachiyomi.domain.source.service.HiddenSourceIds

class GetNextChaptersTest {

    private val getEntry = mockk<GetEntry>()
    private val entryChapterRepository = mockk<EntryChapterRepository>()
    private val historyRepository = mockk<HistoryRepository>()
    private val hiddenSourceIds = mockk<HiddenSourceIds>()

    private val getNextChapters = GetNextChapters(
        getEntry = getEntry,
        entryChapterRepository = entryChapterRepository,
        historyRepository = historyRepository,
        hiddenSourceIds = hiddenSourceIds,
    )

    @Test
    fun `await sorts chapters into reading order`() = runTest {
        val entryId = 1L
        val entry = Entry.create().copy(
            id = entryId,
            chapterFlags = Entry.CHAPTER_SORT_DESC or Entry.CHAPTER_SORTING_SOURCE,
        )
        val chapters = listOf(
            chapter(id = 105, entryId = entryId, sourceOrder = 0, read = false),
            chapter(id = 104, entryId = entryId, sourceOrder = 1, read = false),
            chapter(id = 103, entryId = entryId, sourceOrder = 2, read = true),
        )

        coEvery { getEntry.await(entryId) } returns entry
        coEvery { entryChapterRepository.getChaptersByEntryIdAwait(entryId, true) } returns chapters

        getNextChapters.await(entryId, onlyUnread = false).map(EntryChapter::id) shouldBe listOf(103L, 104L, 105L)
    }

    @Test
    fun `await from chapter returns following chapters in reading order`() = runTest {
        val entryId = 1L
        val entry = Entry.create().copy(
            id = entryId,
            chapterFlags = Entry.CHAPTER_SORT_DESC or Entry.CHAPTER_SORTING_SOURCE,
        )
        val chapters = listOf(
            chapter(id = 105, entryId = entryId, sourceOrder = 0, read = false),
            chapter(id = 104, entryId = entryId, sourceOrder = 1, read = false),
            chapter(id = 103, entryId = entryId, sourceOrder = 2, read = true),
        )

        coEvery { getEntry.await(entryId) } returns entry
        coEvery { entryChapterRepository.getChaptersByEntryIdAwait(entryId, true) } returns chapters

        getNextChapters.await(entryId, fromChapterId = 104L, onlyUnread = false).map(EntryChapter::id) shouldBe
            listOf(104L, 105L)
    }

    @Test
    fun `await from chapter preserves merged member order`() = runTest {
        val entryId = 1L
        val mergedMemberId = 2L
        val entry = Entry.create().copy(
            id = entryId,
            chapterFlags = Entry.CHAPTER_SORT_ASC or Entry.CHAPTER_SORTING_SOURCE,
        )
        val chapters = listOf(
            chapter(id = 101, entryId = entryId, sourceOrder = 0, read = true),
            chapter(id = 201, entryId = mergedMemberId, sourceOrder = 1, read = false),
        )

        coEvery { getEntry.await(entryId) } returns entry
        coEvery { entryChapterRepository.getChaptersByEntryIdAwait(entryId, true) } returns chapters

        getNextChapters.await(entryId, fromChapterId = 101L, onlyUnread = false).map(EntryChapter::id) shouldBe
            listOf(201L)
    }

    @Test
    fun `await from chapter respects sort aware merged reading order`() = runTest {
        val entryId = 1L
        val mergedMemberId = 2L
        val entry = Entry.create().copy(
            id = entryId,
            chapterFlags = Entry.CHAPTER_SORT_DESC or Entry.CHAPTER_SORTING_NUMBER,
        )
        val chapters = listOf(
            chapter(id = 101, entryId = entryId, sourceOrder = 0, chapterNumber = 1.0, read = false),
            chapter(id = 203, entryId = mergedMemberId, sourceOrder = 0, chapterNumber = 3.0, read = true),
            chapter(id = 202, entryId = mergedMemberId, sourceOrder = 1, chapterNumber = 2.0, read = false),
            chapter(id = 201, entryId = mergedMemberId, sourceOrder = 2, chapterNumber = 1.0, read = false),
        )

        coEvery { getEntry.await(entryId) } returns entry
        coEvery { entryChapterRepository.getChaptersByEntryIdAwait(entryId, true) } returns chapters

        getNextChapters.await(entryId, onlyUnread = false).map(EntryChapter::id) shouldBe
            listOf(201L, 202L, 203L, 101L)

        getNextChapters.await(entryId, fromChapterId = 203L, onlyUnread = false).map(EntryChapter::id) shouldBe
            listOf(101L)
    }

    @Test
    fun `await from chapter goes from prologue to next merged chapter`() = runTest {
        val entryId = 1L
        val mergedMemberId = 2L
        val entry = Entry.create().copy(
            id = entryId,
            chapterFlags = Entry.CHAPTER_SORT_DESC or Entry.CHAPTER_SORTING_SOURCE,
        )
        val chapters = listOf(
            chapter(id = 101, entryId = entryId, sourceOrder = 2, chapterNumber = 1.0, read = false),
            chapter(id = 100, entryId = mergedMemberId, sourceOrder = 5, chapterNumber = 0.0, read = true),
            chapter(id = 201, entryId = mergedMemberId, sourceOrder = 4, chapterNumber = 1.0, read = false),
            chapter(id = 202, entryId = mergedMemberId, sourceOrder = 3, chapterNumber = 2.0, read = false),
        )

        coEvery { getEntry.await(entryId) } returns entry
        coEvery { entryChapterRepository.getChaptersByEntryIdAwait(entryId, true) } returns chapters

        getNextChapters.await(entryId, onlyUnread = false).map(EntryChapter::id) shouldBe
            listOf(100L, 201L, 202L, 101L)

        getNextChapters.await(entryId, fromChapterId = 100L, onlyUnread = false).map(EntryChapter::id) shouldBe
            listOf(201L, 202L, 101L)
    }

    private fun chapter(
        id: Long,
        entryId: Long,
        sourceOrder: Long,
        chapterNumber: Double = -1.0,
        read: Boolean,
    ): EntryChapter {
        return EntryChapter.create().copy(
            id = id,
            entryId = entryId,
            sourceOrder = sourceOrder,
            chapterNumber = chapterNumber,
            read = read,
            name = "Chapter $id",
            url = "/chapter/$id",
        )
    }
}
