package eu.kanade.domain.track.interactor

import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.track.EnhancedTracker
import eu.kanade.tachiyomi.data.track.Tracker
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.test.DummyTracker
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import mihon.entry.interactions.EntryConsumptionInteraction
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.entry.repository.EntryRepository
import tachiyomi.domain.track.interactor.InsertTrack
import tachiyomi.domain.track.model.EntryTrack

class SyncChapterProgressWithTrackTest {

    private val entryRepository = mockk<EntryRepository>()
    private val entryChapterRepository = mockk<EntryChapterRepository>()
    private val entryConsumptionInteraction = mockk<EntryConsumptionInteraction>()
    private val insertTrack = mockk<InsertTrack>()
    private val interactor = SyncChapterProgressWithTrack(
        entryRepository = entryRepository,
        entryChapterRepository = entryChapterRepository,
        entryConsumptionInteraction = entryConsumptionInteraction,
        insertTrack = insertTrack,
    )

    @Test
    fun `marks remote progress chapters consumed through entry interaction`() = runTest {
        val entry = Entry.create().copy(id = 1L, type = EntryType.MANGA)
        val chapters = listOf(
            chapter(id = 1L, chapterNumber = 1.0, read = true),
            chapter(id = 2L, chapterNumber = 2.0, read = false),
            chapter(id = 3L, chapterNumber = 3.0, read = false),
        )
        val track = track(entryId = 1L, progress = 2.0)
        val tracker = TestEnhancedTracker()

        coEvery { entryRepository.getEntryById(1L) } returns entry
        coEvery { entryChapterRepository.getChaptersByEntryIdAwait(1L) } returns chapters
        coEvery { entryConsumptionInteraction.setConsumed(entry, listOf(chapters[1]), consumed = true) } just Runs
        coEvery { insertTrack.await(track) } just Runs

        interactor.await(entryId = 1L, remoteTrack = track, tracker = tracker)

        coVerify(exactly = 1) {
            entryConsumptionInteraction.setConsumed(entry, listOf(chapters[1]), consumed = true)
        }
        coVerify(exactly = 0) { entryChapterRepository.updateAll(any()) }
    }

    @Test
    fun `does not consume chapters when tracker does not support entry type`() = runTest {
        val entry = Entry.create().copy(id = 1L, type = EntryType.ANIME)
        val tracker = TestEnhancedTracker(supportedEntryTypes = setOf(EntryType.MANGA))

        coEvery { entryRepository.getEntryById(1L) } returns entry

        interactor.await(entryId = 1L, remoteTrack = track(entryId = 1L, progress = 2.0), tracker = tracker)

        coVerify(exactly = 0) { entryChapterRepository.getChaptersByEntryIdAwait(any()) }
        coVerify(exactly = 0) { entryConsumptionInteraction.setConsumed(any(), any(), any()) }
    }

    private fun chapter(id: Long, chapterNumber: Double, read: Boolean): EntryChapter {
        return EntryChapter.create().copy(
            id = id,
            entryId = 1L,
            chapterNumber = chapterNumber,
            read = read,
            name = "Chapter $chapterNumber",
            url = "/chapter/$id",
        )
    }

    private fun track(entryId: Long, progress: Double): EntryTrack {
        return EntryTrack(
            id = 1L,
            entryId = entryId,
            trackerId = 1L,
            remoteId = 1L,
            libraryId = null,
            title = "Title",
            progress = progress,
            total = 0L,
            status = 1L,
            score = 0.0,
            remoteUrl = "",
            startDate = 0L,
            finishDate = 0L,
            private = false,
        )
    }

    private class TestEnhancedTracker(
        override val supportedEntryTypes: Set<EntryType> = setOf(EntryType.MANGA),
    ) : Tracker by DummyTracker(id = 1L, name = "Tracker"), EnhancedTracker {

        override suspend fun update(track: Track, didReadChapter: Boolean): Track = track

        override fun getAcceptedSources(): List<String> = emptyList()

        override fun loginNoop() = Unit

        override suspend fun match(entry: Entry) = null

        override fun isTrackFrom(
            track: EntryTrack,
            entry: Entry,
            source: eu.kanade.tachiyomi.data.track.EntryTrackingSource?,
        ): Boolean = false

        override fun migrateTrack(
            track: EntryTrack,
            entry: Entry,
            newSource: eu.kanade.tachiyomi.data.track.EntryTrackingSource,
        ): EntryTrack? = null
    }
}
