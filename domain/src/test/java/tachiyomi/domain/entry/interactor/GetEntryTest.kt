package tachiyomi.domain.entry.interactor

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.repository.EntryRepository

class GetEntryTest {
    @Test
    fun `await preserves coroutine cancellation`() = runTest {
        val repository = mockk<EntryRepository> {
            coEvery { getEntryById(1L) } throws CancellationException("superseded")
        }

        shouldThrow<CancellationException> { GetEntry(repository).await(1L) }
    }

    @Test
    fun `batch await restores requested order and omits missing entries`() = runTest {
        val first = Entry.create().copy(id = 1L)
        val third = Entry.create().copy(id = 3L)
        val repository = mockk<EntryRepository> {
            coEvery { getEntriesByIds(listOf(3L, 2L, 1L)) } returns listOf(first, third)
        }

        GetEntry(repository).await(listOf(3L, 2L, 1L, 3L)) shouldContainExactly listOf(third, first)
    }

    @Test
    fun `batch failure falls back to independent reads`() = runTest {
        val first = Entry.create().copy(id = 1L)
        val third = Entry.create().copy(id = 3L)
        val repository = mockk<EntryRepository> {
            coEvery { getEntriesByIds(listOf(1L, 2L, 3L)) } throws IllegalStateException("batch failed")
            coEvery { getEntryById(1L) } returns first
            coEvery { getEntryById(2L) } throws IllegalStateException("row failed")
            coEvery { getEntryById(3L) } returns third
        }

        GetEntry(repository).await(listOf(1L, 2L, 3L)) shouldContainExactly listOf(first, third)
    }

    @Test
    fun `batch await preserves coroutine cancellation without fallback`() = runTest {
        val repository = mockk<EntryRepository> {
            coEvery { getEntriesByIds(listOf(1L, 2L)) } throws CancellationException("superseded")
        }

        shouldThrow<CancellationException> { GetEntry(repository).await(listOf(1L, 2L)) }
    }
}
