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
import mihon.domain.chapter.interactor.FilterEntryChaptersForDownload
import mihon.entry.interactions.EntryBulkDownloadAction
import mihon.entry.interactions.EntryBulkDownloadCandidateResult
import mihon.entry.interactions.book.download.BookDownloadCache
import mihon.entry.interactions.book.download.BookDownloadManager
import mihon.entry.interactions.book.download.BookDownloadPackageKey
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.interactor.GetEntryWithChapters
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.repository.EntryRepository
import tachiyomi.domain.entry.repository.MergedEntryRepository
import tachiyomi.domain.source.service.SourceManager
import kotlin.test.assertEquals
import kotlin.test.assertIs

class BookDownloadProcessorTest {
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
    fun `next bulk download respects merged reading order and limit`() = runTest {
        val visible = entry(id = 1L, source = 10L).copy(
            chapterFlags = Entry.CHAPTER_SORT_DESC or Entry.CHAPTER_SORTING_NUMBER,
        )
        val chapters = listOf(
            chapter(id = 11L, entryId = 1L, number = 1.0),
            chapter(id = 23L, entryId = 2L, number = 3.0),
            chapter(id = 22L, entryId = 2L, number = 2.0),
            chapter(id = 21L, entryId = 2L, number = 1.0),
        )
        val fixture = fixture(
            entries = mapOf(2L to entry(id = 2L, source = 20L)),
            chapters = chapters,
        )

        val result = fixture.processor.resolveBulkDownloadCandidates(
            entry = visible,
            action = EntryBulkDownloadAction.next(2),
            candidates = null,
            memberEntryIds = listOf(1L, 2L),
        )

        assertEquals(
            listOf(21L, 22L),
            assertIs<EntryBulkDownloadCandidateResult.Supported>(result).chapters.map(EntryChapter::id),
        )
    }

    @Test
    fun `automatic downloads use the shared category and unread filter`() = runTest {
        val entry = entry(id = 1L, source = 10L)
        val first = chapter(id = 11L, entryId = entry.id, number = 1.0)
        val second = chapter(id = 12L, entryId = entry.id, number = 2.0)
        val fixture = fixture()
        coEvery { fixture.filter.await(entry, listOf(first, second)) } returns listOf(second)

        val result = fixture.processor.filterAutoDownloadCandidates(entry, listOf(first, second))

        assertEquals(listOf(second), result)
        coVerify(exactly = 1) { fixture.filter.await(entry, listOf(first, second)) }
    }

    @Test
    fun `deleting a merged entry includes every indexed member`() = runTest {
        val visible = entry(id = 1L, source = 10L)
        val fixture = fixture()
        every { fixture.cache.memberEntryIds(visible.id) } returns setOf(1L, 2L)

        fixture.processor.deleteEntryDownloads(visible)

        coVerify(exactly = 1) { fixture.manager.deleteEntryDownloads(visible, setOf(1L, 2L)) }
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

        val result = fixture.processor.resolveBulkDownloadCandidates(
            entry = entry,
            action = EntryBulkDownloadAction.unread,
            candidates = listOf(downloaded, available),
            memberEntryIds = emptyList(),
        )

        assertEquals(
            listOf(available),
            assertIs<EntryBulkDownloadCandidateResult.Supported>(result).chapters,
        )
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
            coEvery { awaitChapters(any(), any()) } returns chapters
        }
        val filter = mockk<FilterEntryChaptersForDownload>()
        val mergedEntryRepository = mockk<MergedEntryRepository> {
            every { subscribeAll() } returns flowOf(emptyList())
        }
        val processor = BookDownloadProcessor(
            BookDownloadProcessorDependencies(
                manager = manager,
                cache = cache,
                sourceManager = mockk<SourceManager>(),
                entryRepository = entryRepository,
                getEntryWithChapters = getEntryWithChapters,
                filterEntryChaptersForDownload = filter,
                mergedEntryRepository = mergedEntryRepository,
            ),
        )
        return ProcessorFixture(processor, manager, cache, filter)
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
    val filter: FilterEntryChaptersForDownload,
)
