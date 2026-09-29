package mihon.entry.interactions.book.download

import android.content.Context
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import mihon.entry.interactions.book.document.preparation.BookDocumentPreparedCache
import mihon.entry.interactions.book.document.resource.BookPublicationResourceGatewayFactory
import mihon.entry.interactions.book.download.model.BookDownload
import mihon.entry.interactions.download.EntryDownloadWorkController
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame

class BookDownloadManagerTest {
    @Test
    fun `queue changes made during restoration win without duplicating children`() {
        val restoredFirst = download(1L)
        val restoredReplaced = download(2L)
        val currentReplacement = download(2L)
        val currentThird = download(3L)

        val merged = mergeRestoredBookDownloads(
            restored = listOf(restoredFirst, restoredReplaced),
            current = listOf(currentReplacement, currentThird),
        )

        assertEquals(listOf(1L, 2L, 3L), merged.map { it.chapter.id })
        assertSame(currentReplacement, merged[1])
    }

    @Test
    fun `pausing a finalizing book preserves it for retry`() = runTest {
        val downloadStarted = CompletableDeferred<Unit>()
        val downloader = mockk<BookDownloader> {
            coEvery { download(any()) } coAnswers {
                firstArg<BookDownload>().status = BookDownload.State.FINALIZING
                downloadStarted.complete(Unit)
                awaitCancellation()
            }
        }
        val manager = manager(downloader)
        val entry = Entry.create().copy(
            id = 1L,
            source = 42L,
            url = "/book",
            title = "Book",
        )
        val chapter = chapter(id = 11L, sourceOrder = 1L)
        manager.queueBooks(entry, listOf(chapter), autoStart = false)
        val worker = launch { manager.runDownloads() }
        downloadStarted.await()

        manager.startDownloads()
        assertEquals(BookDownload.State.FINALIZING, manager.queueState.value.single().status)
        manager.pauseDownloads()
        assertEquals(BookDownload.State.QUEUE, manager.queueState.value.single().status)
        worker.cancelAndJoin()

        assertEquals(BookDownload.State.QUEUE, manager.queueState.value.single().status)
        assertFalse(manager.isRunning.value)
    }

    private fun download(chapterId: Long): BookDownload = mockk {
        every { chapter.id } returns chapterId
    }

    private fun chapter(id: Long, sourceOrder: Long): EntryChapter = EntryChapter.create().copy(
        id = id,
        entryId = 1L,
        sourceOrder = sourceOrder,
        url = "/chapter/$id",
        name = "Chapter $id",
    )

    private fun manager(
        downloader: BookDownloader,
        workController: EntryDownloadWorkController = mockk(relaxed = true),
    ): BookDownloadManager {
        val appContext = mockk<Context>(relaxed = true)
        val context = mockk<Context> {
            every { applicationContext } returns appContext
        }
        val cache = mockk<BookDownloadCache> {
            coEvery { ensureInitialized() } returns Unit
            every { isDownloaded(any()) } returns false
            every { changes } returns emptyFlow()
        }
        val store = mockk<BookDownloadStore>(relaxed = true) {
            coEvery { restore() } returns emptyList()
        }
        return BookDownloadManager(
            context = context,
            cache = cache,
            provider = mockk(relaxed = true),
            downloader = downloader,
            preparedDocumentCache = mockk<BookDocumentPreparedCache>(relaxed = true),
            resourceGatewayFactory = mockk<BookPublicationResourceGatewayFactory>(relaxed = true),
            sourceManager = mockk(relaxed = true),
            store = store,
            workController = workController,
        )
    }
}
