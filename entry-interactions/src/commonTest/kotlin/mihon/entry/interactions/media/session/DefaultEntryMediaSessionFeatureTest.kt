package mihon.entry.interactions.media.session

import eu.kanade.tachiyomi.source.entry.EntryType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import mihon.entry.interactions.download.EntryDownloadLifecycleEvent
import mihon.entry.interactions.download.EntryDownloadLifecycleFeature
import mihon.entry.interactions.history.EntryHistoryFeature
import mihon.entry.interactions.state.EntryProgressFeature
import mihon.entry.interactions.tracking.EntryTrackingFeature
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.model.EntryProgressLocator
import tachiyomi.domain.entry.model.EntryProgressState
import tachiyomi.domain.entry.repository.EntryRepository

class DefaultEntryMediaSessionFeatureTest {
    @Test
    fun `an incognito source records no progress, history or tracking but still drives downloads`() = runTest {
        val event = progressedEvent()
        val repository = mockk<EntryRepository> {
            coEvery { getEntryById(event.child.entryId) } returns event.visibleEntry
        }
        val progress = mockk<EntryProgressFeature>(relaxed = true)
        val downloads = mockk<EntryDownloadLifecycleFeature>(relaxed = true)
        val history = mockk<EntryHistoryFeature>(relaxed = true)
        val tracking = mockk<EntryTrackingFeature>(relaxed = true)
        val types = setOf(EntryType.MANGA)
        val feature = DefaultEntryMediaSessionFeature(
            types = EntryMediaSessionTypes(
                mediaSession = types,
                progress = types,
                download = types,
                translateAhead = emptySet(),
            ),
            incognitoState = { sourceId -> sourceId == event.visibleEntry.source },
            entryRepository = { repository },
            progress = { progress },
            downloads = { downloads },
            history = { history },
            tracking = { tracking },
            automaticTrackingSynchronizationEnabled = { true },
            translateAhead = { error("Translate ahead does not apply") },
        )

        feature.onEvent(event)

        coVerify(exactly = 0) { progress.recordMediaProgress(any()) }
        coVerify(exactly = 0) { history.record(any(), any()) }
        coVerify(exactly = 0) { tracking.synchronizeProgress(any(), any(), any()) }
        coVerify { downloads.onEvent(any<EntryDownloadLifecycleEvent.Progressed>()) }
    }

    private fun progressedEvent(): EntryMediaSessionEvent.Progressed {
        val entry = Entry.create().copy(id = 91L, source = 17L, type = EntryType.MANGA)
        val child = EntryChapter.create().copy(id = 92L, entryId = entry.id, url = "/child", chapterNumber = 3.0)
        return EntryMediaSessionEvent.Progressed(
            visibleEntry = entry,
            child = child,
            progress = EntryProgressState(
                entryId = entry.id,
                chapterId = child.id,
                resourceKey = child.url,
                locator = EntryProgressLocator(kind = "page", progression = 1.0),
                completed = true,
                locatorUpdatedAt = 2_000L,
                completionUpdatedAt = 2_000L,
            ),
            fraction = 1.0,
            activity = EntryMediaSessionActivity(
                sessionId = "session",
                sequence = 0L,
                startedAtEpochMillis = 1_000L,
                recordedAtEpochMillis = 2_000L,
                durationMillis = 1_000L,
                timeZoneId = "UTC",
            ),
        )
    }
}
