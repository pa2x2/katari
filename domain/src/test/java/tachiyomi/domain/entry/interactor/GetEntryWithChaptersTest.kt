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
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.entry.service.EntryChildOwnershipResolution
import tachiyomi.domain.entry.service.EntryChildOwnershipResolutionPort

class GetEntryWithChaptersTest {

    private val entryChapterRepository = mockk<EntryChapterRepository>()
    private val childOwnership = mockk<EntryChildOwnershipResolutionPort>()

    private val getEntryWithChapters = GetEntryWithChapters(
        entryChapterRepository = entryChapterRepository,
        childOwnership = childOwnership,
    )

    @Test
    fun `subscribe updates when non-target merged member chapters change`() = runTest {
        val entryId = 1L
        val mergedMemberId = 2L
        val entry = Entry.create().copy(id = entryId, profileId = 7L)
        val member = Entry.create().copy(id = mergedMemberId, profileId = 7L)
        val ownershipFlow = MutableStateFlow(
            EntryChildOwnershipResolution(
                profileId = 7L,
                requestedEntryId = entryId,
                visibleEntryId = entryId,
                orderedOwners = listOf(entry, member),
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

        every { childOwnership.observeChildOwnership(7L, entryId) } returns ownershipFlow
        every { entryChapterRepository.getChaptersByEntryId(entryId) } returns targetChaptersFlow
        every { entryChapterRepository.getChaptersByEntryId(mergedMemberId) } returns mergedChaptersFlow

        val emissions = mutableListOf<Pair<Entry, List<EntryChapter>>>()

        val job = launch {
            getEntryWithChapters.subscribe(entry)
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
    fun `subscribe updates when child ownership changes`() = runTest {
        val entry = Entry.create().copy(id = 1L, profileId = 7L)
        val member = Entry.create().copy(id = 2L, profileId = 7L)
        val ownershipFlow = MutableStateFlow(
            EntryChildOwnershipResolution(
                profileId = 7L,
                requestedEntryId = entry.id,
                visibleEntryId = entry.id,
                orderedOwners = listOf(entry, member),
            ),
        )
        every { childOwnership.observeChildOwnership(7L, entry.id) } returns ownershipFlow
        every { entryChapterRepository.getChaptersByEntryId(entry.id) } returns MutableStateFlow(
            listOf(chapter(id = 101L, entryId = entry.id, sourceOrder = 1L)),
        )
        every { entryChapterRepository.getChaptersByEntryId(member.id) } returns MutableStateFlow(
            listOf(chapter(id = 201L, entryId = member.id, sourceOrder = 1L)),
        )
        val emissions = mutableListOf<Pair<Entry, List<EntryChapter>>>()

        val job = launch {
            getEntryWithChapters.subscribe(entry).take(2).toList(emissions)
        }
        advanceUntilIdle()

        ownershipFlow.value = ownershipFlow.value.copy(orderedOwners = listOf(entry))
        advanceUntilIdle()

        emissions.map { emission -> emission.second.map(EntryChapter::id) } shouldBe listOf(
            listOf(101L, 201L),
            listOf(101L),
        )
        job.join()
    }

    @Test
    fun `subscribe preserves an ownership change when the child result is equal`() = runTest {
        val entry = Entry.create().copy(id = 1L, profileId = 7L)
        val emptyMember = Entry.create().copy(id = 2L, profileId = 7L)
        val ownershipFlow = MutableStateFlow(
            EntryChildOwnershipResolution(
                profileId = 7L,
                requestedEntryId = entry.id,
                visibleEntryId = entry.id,
                orderedOwners = listOf(entry, emptyMember),
            ),
        )
        val chapters = listOf(chapter(id = 101L, entryId = entry.id, sourceOrder = 1L))
        every { childOwnership.observeChildOwnership(7L, entry.id) } returns ownershipFlow
        every { entryChapterRepository.getChaptersByEntryId(entry.id) } returns MutableStateFlow(chapters)
        every { entryChapterRepository.getChaptersByEntryId(emptyMember.id) } returns MutableStateFlow(emptyList())
        val emissions = mutableListOf<Pair<Entry, List<EntryChapter>>>()

        val job = launch {
            getEntryWithChapters.subscribe(entry).take(2).toList(emissions)
        }
        advanceUntilIdle()

        ownershipFlow.value = ownershipFlow.value.copy(orderedOwners = listOf(entry))
        advanceUntilIdle()

        emissions shouldBe listOf(entry to chapters, entry to chapters)
        job.join()
    }

    @Test
    fun `subscribe bypasses merged chapters when requested`() = runTest {
        val entryId = 1L
        val mergedMemberId = 2L
        val entry = Entry.create().copy(id = entryId, profileId = 7L)
        val targetChaptersFlow = MutableStateFlow(
            listOf(
                chapter(id = 101, entryId = entryId, sourceOrder = 1),
            ),
        )

        every { entryChapterRepository.getChaptersByEntryId(entryId) } returns targetChaptersFlow

        val emissions = mutableListOf<Pair<Entry, List<EntryChapter>>>()

        val job = launch {
            getEntryWithChapters.subscribe(entry, bypassMerge = true)
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
        val entry = Entry.create().copy(id = entryId, profileId = 7L)
        val chapters = listOf(
            chapter(id = 101, entryId = entryId, sourceOrder = 1),
        )

        coEvery { entryChapterRepository.getChaptersByEntryIdAwait(entryId) } returns chapters

        getEntryWithChapters.awaitChapters(entry, bypassMerge = true) shouldBe chapters
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
