package mihon.entry.interactions

import android.content.Context
import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
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

class EntryDownloadNotificationManagerTest {
    @Test
    fun `unified queue drives progress and paused notification states`() = runTest {
        val fixture = fixture(backgroundScope)
        val progress = slot<EntryDownloadProgressNotification>()
        every { fixture.presenter.showProgress(capture(progress)) } returns Unit
        fixture.manager.start()
        runCurrent()

        fixture.state.value = EntryDownloadRuntimeState(queue = listOf(group(item())), isRunning = true)
        runCurrent()

        progress.captured.destination shouldBe EntryDownloadEntryIdentity(1L, EntryType.BOOK, 42L)
        progress.captured.title shouldBe "Entry"
        progress.captured.text shouldBe "Chapter • 25%"
        coVerify { fixture.ownership.resolveDownloadOwners(EntryMergeSubject(1L, 42L)) }

        fixture.state.value = fixture.state.value.copy(isPaused = true)
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
                entryIdentity = EntryDownloadEntryIdentity(
                    profileId = 3L,
                    entryType = EntryType.ANIME,
                    entryId = 7L,
                ),
                title = "Series",
                subtitle = "Episode",
                message = EntryDownloadMessage.Text("Failure"),
            ),
        )
        runCurrent()

        error.captured.destination shouldBe EntryDownloadEntryIdentity(3L, EntryType.ANIME, 7L)
        error.captured.title shouldBe "Series: Episode"
        error.captured.message shouldBe "Failure"
        coVerify { fixture.ownership.resolveDownloadOwners(EntryMergeSubject(3L, 7L)) }
    }

    private fun fixture(scope: CoroutineScope): Fixture {
        val stateFlow = MutableStateFlow(EntryDownloadRuntimeState())
        val progress = MutableSharedFlow<EntryDownloadQueueItem>()
        val status = MutableSharedFlow<EntryDownloadQueueItem>()
        val events = MutableSharedFlow<EntryDownloadEvent>()
        val downloads = mockk<EntryDownloadRuntimeCoordinator>(relaxed = true) {
            every { state } returns stateFlow
            every { queueProgressUpdates() } returns progress
            every { queueStatusUpdates() } returns status
            every { events() } returns events
        }
        val presenter = mockk<EntryDownloadNotificationPresenter>(relaxed = true)
        val ownership = mockk<EntryMergeDownloadOwnershipProjection> {
            coEvery { resolveDownloadOwners(any()) } answers {
                val subject = firstArg<EntryMergeSubject>()
                EntryMergeDownloadOwners(subject.profileId, subject.entryId, emptyList())
            }
        }
        return Fixture(
            manager = EntryDownloadNotificationManager(
                context = mockk<Context>(relaxed = true),
                downloads = downloads,
                actions = mockk(relaxed = true),
                ownership = ownership,
                presenter = presenter,
                scope = scope,
                messageResolver = { message ->
                    when (message) {
                        is EntryDownloadMessage.Text -> message.value
                        is EntryDownloadMessage.Resource -> when (message.resource) {
                            tachiyomi.i18n.MR.strings.download_progress_percent -> "${message.args.single()}%"
                            else -> message.resource.resourceId.toString()
                        }
                    }
                },
            ),
            presenter = presenter,
            ownership = ownership,
            state = stateFlow,
            events = events,
        )
    }

    private fun item() = EntryDownloadQueueItem(
        identity = EntryDownloadIdentity(
            profileId = 1L,
            entryType = EntryType.BOOK,
            entryId = 42L,
            sourceId = 1L,
            childId = 11L,
        ),
        state = EntryDownloadState.DOWNLOADING,
        title = "Entry",
        subtitle = "Chapter",
        dateUpload = 0L,
        chapterNumber = 1.0,
        progress = 25,
        progressMax = 100,
        presentation = EntryDownloadPresentation(
            phase = EntryDownloadPhase.TRANSFERRING,
            progress = EntryDownloadProgress.Percent(25),
        ),
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
        val ownership: EntryMergeDownloadOwnershipProjection,
        val state: MutableStateFlow<EntryDownloadRuntimeState>,
        val events: MutableSharedFlow<EntryDownloadEvent>,
    )
}
