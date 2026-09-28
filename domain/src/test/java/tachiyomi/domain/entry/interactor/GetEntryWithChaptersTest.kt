package tachiyomi.domain.entry.interactor

import io.kotest.matchers.shouldBe
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
    fun `subscribe updates when merged member chapters or child ownership change`() = runTest {
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
        val memberChaptersFlow = MutableStateFlow(listOf(chapter(id = 201L, entryId = member.id, sourceOrder = 1L)))
        every { childOwnership.observeChildOwnership(7L, entry.id) } returns ownershipFlow
        every { entryChapterRepository.getChaptersByEntryId(entry.id) } returns MutableStateFlow(
            listOf(chapter(id = 101L, entryId = entry.id, sourceOrder = 1L)),
        )
        every { entryChapterRepository.getChaptersByEntryId(member.id) } returns memberChaptersFlow
        val emissions = mutableListOf<Pair<Entry, List<EntryChapter>>>()

        val job = launch {
            getEntryWithChapters.subscribe(entry).take(3).toList(emissions)
        }
        advanceUntilIdle()

        memberChaptersFlow.value += chapter(id = 202L, entryId = member.id, sourceOrder = 2L)
        advanceUntilIdle()
        ownershipFlow.value = ownershipFlow.value.copy(orderedOwners = listOf(entry))
        advanceUntilIdle()

        emissions.map { emission -> emission.second.map(EntryChapter::id) } shouldBe listOf(
            listOf(101L, 201L),
            listOf(101L, 201L, 202L),
            listOf(101L),
        )
        job.join()
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
