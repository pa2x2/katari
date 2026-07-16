package tachiyomi.domain.entry.interactor

import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.repository.EntryRepository

class SetEntryFavoriteTest {

    private val repository = mockk<EntryRepository>()
    private val setEntryFavorite = SetEntryFavorite(repository, now = { 1234L })

    @Test
    fun `adding entry to library records date added`() = runTest {
        val updatedEntry = slot<Entry>()
        coEvery { repository.getEntryById(1L) } returns Entry.create().copy(id = 1L)
        coEvery { repository.update(capture(updatedEntry)) } returns true

        setEntryFavorite.await(1L, true) shouldBe true

        updatedEntry.captured.favorite shouldBe true
        updatedEntry.captured.dateAdded shouldBe 1234L
    }

    @Test
    fun `removing entry from library clears date added`() = runTest {
        val updatedEntry = slot<Entry>()
        coEvery {
            repository.getEntryById(1L)
        } returns Entry.create().copy(id = 1L, favorite = true, dateAdded = 1000L)
        coEvery { repository.update(capture(updatedEntry)) } returns true

        setEntryFavorite.await(1L, false) shouldBe true

        updatedEntry.captured.favorite shouldBe false
        updatedEntry.captured.dateAdded shouldBe 0L
    }

    @Test
    fun `missing entry is not updated`() = runTest {
        coEvery { repository.getEntryById(1L) } returns null

        setEntryFavorite.await(1L, true) shouldBe false

        coVerify(exactly = 0) { repository.update(any()) }
    }
}
