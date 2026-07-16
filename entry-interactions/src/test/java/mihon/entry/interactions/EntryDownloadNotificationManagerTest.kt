package mihon.entry.interactions

import android.content.Context
import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import mihon.entry.interactions.download.notification.EntryDownloadErrorNotification
import mihon.entry.interactions.download.notification.EntryDownloadNotificationPresenter
import mihon.entry.interactions.download.notification.EntryDownloadProgressNotification
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.interactor.GetMergedEntry

class EntryDownloadNotificationManagerTest {
    @Test
    fun `unified queue drives progress and paused notification states`() = runTest {
        val fixture = fixture(backgroundScope)
        val progress = slot<EntryDownloadProgressNotification>()
        every { fixture.presenter.showProgress(capture(progress)) } returns Unit
        fixture.manager.start()
        runCurrent()

        fixture.queue.value = listOf(group(item()))
        fixture.running.value = true
        runCurrent()

        progress.captured.entryType shouldBe EntryType.BOOK
        progress.captured.entryId shouldBe 42L
        progress.captured.title shouldBe "Entry"
        progress.captured.text shouldBe "Chapter • 25%"

        fixture.paused.value = true
        runCurrent()

        verify { fixture.presenter.showPaused() }
    }

    @Test
    fun `processor error information is rendered by the central manager`() = runTest {
        val fixture = fixture(backgroundScope)
        val error = slot<EntryDownloadErrorNotification>()
        every { fixture.presenter.showError(capture(error)) } returns Unit
        fixture.manager.start()
        runCurrent()

        fixture.events.emit(
            EntryDownloadEvent.Error(
                entryType = EntryType.ANIME,
                entryId = 7L,
                title = "Series",
                subtitle = "Episode",
                message = EntryDownloadMessage.Text("Failure"),
            ),
        )
        runCurrent()

        error.captured.entryType shouldBe EntryType.ANIME
        error.captured.entryId shouldBe 7L
        error.captured.title shouldBe "Series: Episode"
        error.captured.message shouldBe "Failure"
    }

    private fun fixture(scope: CoroutineScope): Fixture {
        val queue = MutableStateFlow<List<EntryDownloadQueueGroup>>(emptyList())
        val running = MutableStateFlow(false)
        val paused = MutableStateFlow(false)
        val progress = MutableSharedFlow<EntryDownloadQueueItem>()
        val status = MutableSharedFlow<EntryDownloadQueueItem>()
        val events = MutableSharedFlow<EntryDownloadEvent>()
        val downloads = mockk<EntryDownloadInteraction>(relaxed = true) {
            every { queueState } returns queue
            every { isRunning } returns running
            every { isPaused } returns paused
            every { queueProgressUpdates() } returns progress
            every { queueStatusUpdates() } returns status
            every { events() } returns events
        }
        val presenter = mockk<EntryDownloadNotificationPresenter>(relaxed = true)
        val getMergedEntry = mockk<GetMergedEntry> {
            coEvery { awaitVisibleTargetId(any()) } answers { firstArg() }
        }
        return Fixture(
            manager = EntryDownloadNotificationManager(
                context = mockk<Context>(relaxed = true),
                downloads = downloads,
                actions = mockk(relaxed = true),
                getMergedEntry = getMergedEntry,
                presenter = presenter,
                scope = scope,
            ),
            presenter = presenter,
            queue = queue,
            running = running,
            paused = paused,
            events = events,
        )
    }

    private fun item() = EntryDownloadQueueItem(
        entryType = EntryType.BOOK,
        state = EntryDownloadState.DOWNLOADING,
        entryId = 42L,
        childId = 11L,
        title = "Entry",
        subtitle = "Chapter",
        dateUpload = 0L,
        chapterNumber = 1.0,
        progress = 25,
        progressMax = 100,
        progressText = "25%",
    )

    private fun group(item: EntryDownloadQueueItem) = EntryDownloadQueueGroup(
        sourceId = 1L,
        sourceName = "Source",
        entryType = item.entryType,
        items = listOf(item),
    )

    private data class Fixture(
        val manager: EntryDownloadNotificationManager,
        val presenter: EntryDownloadNotificationPresenter,
        val queue: MutableStateFlow<List<EntryDownloadQueueGroup>>,
        val running: MutableStateFlow<Boolean>,
        val paused: MutableStateFlow<Boolean>,
        val events: MutableSharedFlow<EntryDownloadEvent>,
    )
}
