package app.katari.sync.host

import app.katari.entry.interactions.download.EntryDownloadActionFeature
import app.katari.entry.interactions.download.EntryDownloadActionResult
import app.katari.entry.interactions.source.EntrySourceRefreshFailure
import app.katari.entry.interactions.source.EntrySourceRefreshFeature
import app.katari.entry.interactions.source.EntrySourceRefreshResult
import app.katari.sync.client.downloadrequest.DownloadRequestResult
import app.katari.sync.client.downloadrequest.ReceivedDownloadRequest
import app.katari.sync.client.downloadrequest.RequestedChapter
import app.katari.sync.ui.notice.DownloadRequestFailure
import app.katari.sync.ui.notice.SyncNotifications
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.entry.repository.EntryRepository
import tachiyomi.domain.source.model.SourceDisplayInfo
import tachiyomi.domain.source.service.SourceManager

class RequestedDownloadsTest {
    private val entry = Entry.create().copy(id = 7, profileId = 1, source = 42, title = "Shadow Slave")
    private val listed = mutableListOf(chapter(1, "/c/1"))
    private val entries = mockk<EntryRepository> { coEvery { getEntryById(7) } returns entry }
    private val chapters = mockk<EntryChapterRepository> {
        coEvery { getChaptersByEntryIdAwait(7, any()) } answers { listed.toList() }
    }
    private val sourceMissing = MutableStateFlow(false)
    private val sources = mockk<SourceManager> {
        every { isInitialized } returns MutableStateFlow(true)
        every { getDisplayInfo(42) } answers {
            SourceDisplayInfo(42, "Webnovel", "en", isMissing = sourceMissing.value, hasKnownName = true)
        }
    }
    private val refresh = mockk<EntrySourceRefreshFeature>()
    private val downloads = mockk<EntryDownloadActionFeature> {
        coEvery { download(any(), any(), any()) } returns EntryDownloadActionResult.Performed
    }
    private val failures = mutableListOf<DownloadRequestFailure>()
    private val notifications = mockk<SyncNotifications> {
        val failure = slot<DownloadRequestFailure>()
        every { showDownloadRequestFailed(capture(failure)) } answers { failures += failure.captured }
    }
    private val performer = RequestedDownloads(entries, chapters, sources, refresh, downloads, notifications)
    private val request = ReceivedDownloadRequest(
        entryId = 7,
        chapters = listOf(RequestedChapter("/c/1", "Chapter 1"), RequestedChapter("/c/2", "Chapter 2")),
        fromName = "ThinkPad",
    )

    @Test
    fun `chapters not listed here are found after one refresh, and all download`() = runTest {
        coEvery { refresh.refresh(any()) } answers {
            listed += chapter(2, "/c/2")
            EntrySourceRefreshResult.Refreshed(emptyList(), 1, 0, 0, false, emptySet())
        }

        performer.perform(request) shouldBe DownloadRequestResult.DONE

        coVerify(exactly = 1) { refresh.refresh(any()) }
        coVerify { downloads.download(entry, listOf(chapter(1, "/c/1"), chapter(2, "/c/2")), false) }
        failures shouldBe emptyList()
    }

    @Test
    fun `chapters the source no longer lists are told, and the rest download`() = runTest {
        coEvery { refresh.refresh(any()) } returns
            EntrySourceRefreshResult.Refreshed(emptyList(), 0, 0, 0, false, emptySet())

        performer.perform(request) shouldBe DownloadRequestResult.DONE

        coVerify { downloads.download(entry, listOf(chapter(1, "/c/1")), false) }
        failures shouldBe listOf(failure(listOf("Chapter 2"), DownloadRequestFailure.Reason.CHAPTERS_GONE))
    }

    @Test
    fun `a missing source is told for every chapter asked for, and nothing downloads`() = runTest {
        sourceMissing.value = true

        performer.perform(request) shouldBe DownloadRequestResult.DONE

        coVerify(exactly = 0) { downloads.download(any(), any(), any()) }
        failures shouldBe
            listOf(failure(listOf("Chapter 1", "Chapter 2"), DownloadRequestFailure.Reason.SOURCE_MISSING))
    }

    @Test
    fun `a refresh that fails leaves the request for later, with nothing downloaded or told`() = runTest {
        coEvery { refresh.refresh(any()) } returns
            EntrySourceRefreshResult.Failed(EntrySourceRefreshFailure.Operation(IllegalStateException("offline")))

        performer.perform(request) shouldBe DownloadRequestResult.TRY_LATER

        coVerify(exactly = 0) { downloads.download(any(), any(), any()) }
        verify(exactly = 0) { notifications.showDownloadRequestFailed(any()) }
    }

    private fun chapter(id: Long, url: String) = EntryChapter.create().copy(id = id, entryId = 7, url = url)

    private fun failure(chapters: List<String>, reason: DownloadRequestFailure.Reason) = DownloadRequestFailure(
        entryId = 7,
        profileId = 1,
        title = "Shadow Slave",
        from = "ThinkPad",
        chapters = chapters,
        sourceName = "Webnovel",
        reason = reason,
    )
}
