package tachiyomi.domain.entry.interactor

import eu.kanade.tachiyomi.source.entry.EntryType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.repository.EntryRepository
import tachiyomi.domain.entry.repository.MergedEntryRepository

class UpdateMergedEntryTest {

    private val repository = mockk<MergedEntryRepository>(relaxed = true)
    private val entryRepository = mockk<EntryRepository>()
    private val updateMergedEntry = UpdateMergedEntry(repository, entryRepository)

    @Test
    fun `allows same-type merge`() = runTest {
        coEvery { entryRepository.getEntryById(1L) } returns entry(1L, EntryType.MANGA)
        coEvery { entryRepository.getEntryById(2L) } returns entry(2L, EntryType.MANGA)

        updateMergedEntry.awaitMerge(targetEntryId = 1L, orderedEntryIds = listOf(1L, 2L))

        coVerify { repository.upsertGroup(1L, listOf(1L, 2L)) }
    }

    @Test
    fun `rejects mixed-type merge`() = runTest {
        coEvery { entryRepository.getEntryById(1L) } returns entry(1L, EntryType.MANGA)
        coEvery { entryRepository.getEntryById(2L) } returns entry(2L, EntryType.ANIME)

        expectIllegalArgument {
            updateMergedEntry.awaitMerge(targetEntryId = 1L, orderedEntryIds = listOf(1L, 2L))
        }
    }

    @Test
    fun `rejects duplicate members`() = runTest {
        expectIllegalArgument {
            updateMergedEntry.awaitMerge(targetEntryId = 1L, orderedEntryIds = listOf(1L, 1L))
        }
    }

    @Test
    fun `rejects target missing from ordered members`() = runTest {
        expectIllegalArgument {
            updateMergedEntry.awaitMerge(targetEntryId = 1L, orderedEntryIds = listOf(2L, 3L))
        }
    }

    private suspend fun expectIllegalArgument(block: suspend () -> Unit) {
        try {
            block()
            throw AssertionError("Expected IllegalArgumentException")
        } catch (_: IllegalArgumentException) {
        }
    }

    private fun entry(id: Long, type: EntryType): Entry {
        return Entry.create().copy(
            id = id,
            title = "Entry $id",
            url = "/entry/$id",
            type = type,
        )
    }
}
