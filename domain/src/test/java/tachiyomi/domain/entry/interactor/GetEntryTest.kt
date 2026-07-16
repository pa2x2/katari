package tachiyomi.domain.entry.interactor

import io.kotest.assertions.throwables.shouldThrow
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.repository.EntryRepository

class GetEntryTest {
    @Test
    fun `await preserves coroutine cancellation`() = runTest {
        val repository = mockk<EntryRepository> {
            coEvery { getEntryById(1L) } throws CancellationException("superseded")
        }

        shouldThrow<CancellationException> { GetEntry(repository).await(1L) }
    }
}
