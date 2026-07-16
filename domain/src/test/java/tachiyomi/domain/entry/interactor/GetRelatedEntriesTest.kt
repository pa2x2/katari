package tachiyomi.domain.entry.interactor

import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.entry.RelatedEntriesSource
import eu.kanade.tachiyomi.source.entry.SEntry
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.repository.EntryRepository
import tachiyomi.domain.source.service.SourceManager

class GetRelatedEntriesTest {

    @Test
    fun `related entries preserve source order and mixed entry types`() = runTest {
        val requestedEntry = slot<SEntry>()
        val source = mockk<RelatedEntriesSource> {
            every { id } returns SOURCE_ID
            coEvery { getRelatedEntries(capture(requestedEntry)) } returns listOf(
                sourceEntry("/manga", "Manga", EntryType.MANGA),
                sourceEntry("/anime", "Anime", EntryType.ANIME),
            )
        }
        val repository = passThroughRepository()
        val interactor = GetRelatedEntries(
            sourceManager = sourceManager(source),
            networkToLocalEntry = NetworkToLocalEntry(repository),
        )

        val result = interactor.await(entry("/book", "Book", EntryType.BOOK))

        requestedEntry.captured.url shouldBe "/book"
        requestedEntry.captured.type shouldBe EntryType.BOOK
        result.map(Entry::type) shouldContainExactly listOf(EntryType.MANGA, EntryType.ANIME)
        result.map(Entry::source).distinct() shouldBe listOf(SOURCE_ID)
    }

    @Test
    fun `duplicate related entry identities are persisted once`() = runTest {
        val source = mockk<RelatedEntriesSource> {
            every { id } returns SOURCE_ID
            coEvery { getRelatedEntries(any()) } returns listOf(
                sourceEntry("/same", "First", EntryType.MANGA),
                sourceEntry("/same", "Duplicate", EntryType.MANGA),
                sourceEntry("/same", "Different type", EntryType.ANIME),
            )
        }
        val repository = passThroughRepository()
        val interactor = GetRelatedEntries(
            sourceManager = sourceManager(source),
            networkToLocalEntry = NetworkToLocalEntry(repository),
        )

        val result = interactor.await(entry("/origin", "Origin", EntryType.MANGA))

        result.map(Entry::type) shouldContainExactly listOf(EntryType.MANGA, EntryType.ANIME)
        coVerify(exactly = 2) { repository.insertOrUpdate(any()) }
    }

    @Test
    fun `source without capability fails before persistence`() = runTest {
        val source = mockk<UnifiedSource> {
            every { id } returns SOURCE_ID
        }
        val repository = passThroughRepository()
        val interactor = GetRelatedEntries(
            sourceManager = sourceManager(source),
            networkToLocalEntry = NetworkToLocalEntry(repository),
        )

        assertThrows<RelatedEntriesNotSupportedException> {
            interactor.await(entry("/origin", "Origin", EntryType.MANGA))
        }
        coVerify(exactly = 0) { repository.insertOrUpdate(any()) }
    }

    private fun sourceManager(source: UnifiedSource): SourceManager = mockk {
        every { get(SOURCE_ID) } returns source
    }

    private fun passThroughRepository(): EntryRepository = mockk {
        coEvery { insertOrUpdate(any()) } answers { firstArg() }
    }

    private fun entry(url: String, title: String, type: EntryType): Entry = Entry.create().copy(
        source = SOURCE_ID,
        url = url,
        title = title,
        type = type,
    )

    private fun sourceEntry(url: String, title: String, type: EntryType): SEntry = SEntry.create().apply {
        this.url = url
        this.title = title
        this.type = type
    }

    private companion object {
        const val SOURCE_ID = 7L
    }
}
