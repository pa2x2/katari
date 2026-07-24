package mihon.entry.interactions.book

import eu.kanade.tachiyomi.source.entry.EntryType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mihon.entry.interactions.EntryDownloadMessage
import mihon.entry.interactions.EntryDownloadPhase
import mihon.entry.interactions.EntryDownloadProgress
import mihon.entry.interactions.book.download.BookDownloadCache
import mihon.entry.interactions.book.download.BookDownloadManager
import mihon.entry.interactions.book.download.BookDownloadPackageKey
import mihon.entry.interactions.book.download.model.BookDownload
import mihon.entry.interactions.book.download.model.BookDownloadFailure
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.interactor.GetEntryWithChapters
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.repository.EntryRepository
import tachiyomi.domain.source.service.SourceManager
import kotlin.test.assertEquals
import kotlin.test.assertIs

class BookDownloadProcessorTest {
    @Test
    fun `book queue mapping emits shared phase progress and localized failure semantics`() {
        val transferring = BookDownload(entry(1L, 10L), chapter(11L, 1L, 1.0)).apply {
            status = BookDownload.State.DOWNLOADING
            progress = 35
        }.toEntryDownloadQueueItem()
        val failed = BookDownload(entry(1L, 10L), chapter(11L, 1L, 1.0)).apply {
            status = BookDownload.State.ERROR
            failure = BookDownloadFailure(BookDownloadFailure.Reason.NETWORK, "raw transport failure")
        }.toEntryDownloadQueueItem()

        assertEquals(EntryDownloadPhase.TRANSFERRING, transferring.presentation.phase)
        assertEquals(EntryDownloadProgress.Percent(35), transferring.presentation.progress)
        assertEquals(EntryDownloadPhase.FAILED, failed.presentation.phase)
        assertEquals(
            EntryDownloadMessage.Resource(tachiyomi.i18n.MR.strings.download_notifier_book_network_error),
            failed.presentation.failure,
        )
    }

    @Test
    fun `merged downloads are queued against each child owner and start once`() = runTest {
        val visible = entry(id = 1L, source = 10L)
        val member = entry(id = 2L, source = 20L)
        val visibleChapter = chapter(id = 11L, entryId = visible.id, number = 1.0)
        val memberChapter = chapter(id = 21L, entryId = member.id, number = 1.0)
        val fixture = fixture(entries = mapOf(member.id to member))

        fixture.processor.download(visible, listOf(visibleChapter, memberChapter), startNow = false)

        coVerify(exactly = 1) { fixture.manager.queueBooks(visible, listOf(visibleChapter), autoStart = false) }
        coVerify(exactly = 1) { fixture.manager.queueBooks(member, listOf(memberChapter), autoStart = false) }
        verify(exactly = 1) { fixture.manager.startDownloads() }
    }

    @Test
    fun `start now promotes every selected book child after owner-aware queueing`() = runTest {
        val visible = entry(id = 1L, source = 10L)
        val member = entry(id = 2L, source = 20L)
        val visibleChapter = chapter(id = 11L, entryId = visible.id, number = 1.0)
        val memberChapter = chapter(id = 21L, entryId = member.id, number = 1.0)
        val fixture = fixture(entries = mapOf(member.id to member))

        fixture.processor.download(visible, listOf(visibleChapter, memberChapter), startNow = true)

        coVerify(exactly = 1) { fixture.manager.queueBooks(visible, listOf(visibleChapter), autoStart = false) }
        coVerify(exactly = 1) { fixture.manager.queueBooks(member, listOf(memberChapter), autoStart = false) }
        verify(exactly = 1) { fixture.manager.startDownloadsNow(listOf(11L, 21L)) }
        verify(exactly = 0) { fixture.manager.startDownloads() }
    }

    @Test
    fun `deleting an entry delegates only its concrete ownership`() = runTest {
        val entry = entry(id = 1L, source = 10L)
        val fixture = fixture()

        fixture.processor.deleteEntryDownloads(entry)

        coVerify(exactly = 1) { fixture.manager.deleteEntryDownloads(entry) }
    }

    @Test
    fun `provided bulk candidates still exclude completed downloads`() = runTest {
        val entry = entry(id = 1L, source = 10L)
        val downloaded = chapter(id = 11L, entryId = entry.id, number = 1.0)
        val available = chapter(id = 12L, entryId = entry.id, number = 2.0)
        val fixture = fixture()
        every {
            fixture.cache.isDownloaded(BookDownloadPackageKey(entry.source, entry.url, downloaded.url))
        } returns true

        val result = fixture.processor.resolveBulkDownloadCandidatePool(
            entry = entry,
            candidates = listOf(downloaded, available),
        )

        assertEquals(listOf(available), result)
    }

    private fun fixture(
        entries: Map<Long, Entry> = emptyMap(),
        chapters: List<EntryChapter> = emptyList(),
    ): ProcessorFixture {
        val manager = mockk<BookDownloadManager>(relaxed = true) {
            every { cacheChanges } returns flowOf(Unit)
            every { queueState } returns MutableStateFlow(emptyList())
            every { isRunning } returns MutableStateFlow(false)
        }
        val cache = mockk<BookDownloadCache>(relaxed = true) {
            every { isInitializing } returns MutableStateFlow(false)
            every { isDownloaded(any()) } returns false
        }
        val entryRepository = mockk<EntryRepository> {
            entries.forEach { (id, entry) -> coEvery { getEntryById(id) } returns entry }
        }
        val getEntryWithChapters = mockk<GetEntryWithChapters> {
            coEvery { awaitChapters(any(), any(), any()) } returns chapters
        }
        val processor = BookDownloadProcessor(
            BookDownloadProcessorDependencies(
                manager = manager,
                cache = cache,
                sourceManager = mockk<SourceManager>(),
                entryRepository = entryRepository,
                getEntryWithChapters = getEntryWithChapters,
            ),
        )
        return ProcessorFixture(processor, manager, cache)
    }

    private fun entry(id: Long, source: Long): Entry = Entry.create().copy(
        id = id,
        profileId = 7L,
        source = source,
        url = "/book/$id",
        title = "Book $id",
        type = EntryType.BOOK,
    )

    private fun chapter(id: Long, entryId: Long, number: Double): EntryChapter = EntryChapter.create().copy(
        id = id,
        entryId = entryId,
        url = "/chapter/$id",
        name = "Chapter $id",
        chapterNumber = number,
    )
}

private data class ProcessorFixture(
    val processor: BookDownloadProcessor,
    val manager: BookDownloadManager,
    val cache: BookDownloadCache,
)
