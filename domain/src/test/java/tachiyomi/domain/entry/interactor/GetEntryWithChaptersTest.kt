package tachiyomi.domain.entry.interactor

import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.model.EntryMerge
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.entry.repository.EntryRepository
import tachiyomi.domain.entry.repository.MergedEntryRepository

class GetEntryWithChaptersTest {

    private val entryRepository = mockk<EntryRepository>()
    private val entryChapterRepository = mockk<EntryChapterRepository>()
    private val mergedEntryRepository = mockk<MergedEntryRepository>()

    private val getEntryWithChapters = GetEntryWithChapters(
        entryRepository = entryRepository,
        entryChapterRepository = entryChapterRepository,
        mergedEntryRepository = mergedEntryRepository,
    )

    @Test
    fun `subscribe updates when non-target merged member chapters change`() = runTest {
        val entryId = 1L
        val mergedMemberId = 2L
        val entry = Entry.create().copy(id = entryId)
        val entryFlow = MutableStateFlow(entry)
        val mergesFlow = MutableStateFlow(
            listOf(
                EntryMerge(targetId = entryId, entryId = entryId, position = 0),
                EntryMerge(targetId = entryId, entryId = mergedMemberId, position = 1),
            ),
        )
        val targetChaptersFlow = MutableStateFlow(
            listOf(
                chapter(id = 101, entryId = entryId, sourceOrder = 1),
            ),
        )
        val mergedChaptersFlow = MutableStateFlow(
            listOf(
                chapter(id = 201, entryId = mergedMemberId, sourceOrder = 1),
            ),
        )

        coEvery { entryRepository.getEntryByIdAsFlow(entryId) } returns entryFlow
        every { mergedEntryRepository.subscribeGroupByEntryId(entryId) } returns mergesFlow
        every { entryChapterRepository.getChaptersByEntryId(entryId) } returns targetChaptersFlow
        every { entryChapterRepository.getChaptersByEntryId(mergedMemberId) } returns mergedChaptersFlow

        val emissions = mutableListOf<Pair<Entry, List<EntryChapter>>>()

        val job = launch {
            getEntryWithChapters.subscribe(entryId)
                .take(2)
                .toList(emissions)
        }

        advanceUntilIdle()

        mergedChaptersFlow.value =
            mergedChaptersFlow.value + chapter(id = 202, entryId = mergedMemberId, sourceOrder = 2)

        advanceUntilIdle()

        emissions.map { emission -> emission.second.map(EntryChapter::id) } shouldBe listOf(
            listOf(101L, 201L),
            listOf(101L, 201L, 202L),
        )

        job.join()
    }

    @Test
    fun `subscribe bypasses merged chapters when requested`() = runTest {
        val entryId = 1L
        val mergedMemberId = 2L
        val entry = Entry.create().copy(id = entryId)
        val entryFlow = MutableStateFlow(entry)
        val mergesFlow = MutableStateFlow(
            listOf(
                EntryMerge(targetId = entryId, entryId = entryId, position = 0),
                EntryMerge(targetId = entryId, entryId = mergedMemberId, position = 1),
            ),
        )
        val targetChaptersFlow = MutableStateFlow(
            listOf(
                chapter(id = 101, entryId = entryId, sourceOrder = 1),
            ),
        )

        coEvery { entryRepository.getEntryByIdAsFlow(entryId) } returns entryFlow
        every { mergedEntryRepository.subscribeGroupByEntryId(entryId) } returns mergesFlow
        every { entryChapterRepository.getChaptersByEntryId(entryId) } returns targetChaptersFlow

        val emissions = mutableListOf<Pair<Entry, List<EntryChapter>>>()

        val job = launch {
            getEntryWithChapters.subscribe(entryId, bypassMerge = true)
                .take(1)
                .toList(emissions)
        }

        advanceUntilIdle()

        emissions.single().second.map(EntryChapter::id) shouldBe listOf(101L)

        job.join()
    }

    @Test
    fun `awaitChapters bypasses merged chapters when requested`() = runTest {
        val entryId = 1L
        val chapters = listOf(
            chapter(id = 101, entryId = entryId, sourceOrder = 1),
        )

        coEvery { entryChapterRepository.getChaptersByEntryIdAwait(entryId) } returns chapters

        getEntryWithChapters.awaitChapters(entryId, bypassMerge = true) shouldBe chapters
    }

    private fun chapter(
        id: Long,
        entryId: Long,
        sourceOrder: Long,
    ): EntryChapter {
        return EntryChapter.create().copy(
            id = id,
            entryId = entryId,
            sourceOrder = sourceOrder,
            name = "Chapter $id",
            url = "/chapter/$id",
        )
    }
}
