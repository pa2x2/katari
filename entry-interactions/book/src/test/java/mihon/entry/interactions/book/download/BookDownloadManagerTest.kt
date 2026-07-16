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
import mihon.entry.interactions.book.download.model.BookDownload
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame

class BookDownloadManagerTest {
    @Test
    fun `queued books preserve resolved next-item reading order`() {
        val entry = Entry.create().copy(id = 1L)
        val chapters = listOf(
            chapter(id = 11L, sourceOrder = 1L),
            chapter(id = 12L, sourceOrder = 2L),
            chapter(id = 13L, sourceOrder = 3L),
        )

        val queued = chapters.toQueuedBookDownloads(entry)

        assertEquals(listOf(11L, 12L, 13L), queued.map { it.chapter.id })
    }

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
    fun `worker cancellation preserves a queued book`() = runTest {
        val downloadStarted = CompletableDeferred<Unit>()
        val downloader = mockk<BookDownloader> {
            coEvery { download(any()) } coAnswers {
                downloadStarted.complete(Unit)
                awaitCancellation()
            }
        }
        val fixture = managerFixture(downloader)
        val entry = Entry.create().copy(
            id = 1L,
            source = 42L,
            url = "/book",
            title = "Book",
        )
        val chapter = chapter(id = 11L, sourceOrder = 1L)
        fixture.manager.queueBooks(entry, listOf(chapter), autoStart = false)
        val worker = launch { fixture.manager.runDownloads() }
        downloadStarted.await()

        fixture.manager.pauseDownloads()
        worker.cancelAndJoin()

        assertEquals(BookDownload.State.QUEUE, fixture.manager.queueState.value.single().status)
        assertFalse(fixture.manager.isRunning.value)
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

    private fun managerFixture(
        downloader: BookDownloader = mockk(relaxed = true),
    ): ManagerFixture {
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
        return ManagerFixture(
            manager = BookDownloadManager(
                context = context,
                cache = cache,
                provider = mockk(relaxed = true),
                downloader = downloader,
                sourceManager = mockk(relaxed = true),
                store = store,
                workController = mockk(relaxed = true),
            ),
        )
    }

    private data class ManagerFixture(
        val manager: BookDownloadManager,
    )
}
