package mihon.entry.interactions.book

import eu.kanade.tachiyomi.source.entry.EntryMedia
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import mihon.book.api.BookContentDescriptor
import mihon.book.api.BookFailureReason
import mihon.entry.interactions.EntryConsumptionStatus
import mihon.entry.interactions.book.download.BookDownloadCleanup
import mihon.entry.interactions.createEntryInteractions
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.InMemoryPreferenceStore
import tachiyomi.domain.entry.interactor.GetEntryWithChapters
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.model.EntryProgressLocator
import tachiyomi.domain.entry.model.EntryProgressState
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.entry.repository.EntryProgressRepository
import tachiyomi.domain.entry.repository.EntryRepository
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BookEntryInteractionPluginTest {
    @Test
    fun `continue starts at first unread chapter in reading order`() = runTest {
        val first = chapter(id = 11L, chapterNumber = 1.0)
        val latest = chapter(id = 12L, chapterNumber = 2.0)
        val getEntryWithChapters = mockk<GetEntryWithChapters> {
            coEvery { awaitChapters(1L, any()) } returns listOf(latest, first)
        }
        val progressRepository = mockk<EntryProgressRepository> {
            coEvery { getByEntryId(1L) } returns emptyList()
        }
        val processor = BookContinueProcessor(
            getEntryWithChapters = getEntryWithChapters,
            entryProgressRepository = progressRepository,
            openProcessor = mockk(),
        )

        assertEquals(first, processor.findNext(entry()))
    }

    @Test
    fun `plugin registers generic book interactions without download support`() = runTest {
        val chapter = chapter()
        val getEntryWithChapters = mockk<GetEntryWithChapters> {
            coEvery { awaitChapters(1L, any()) } returns listOf(chapter)
        }
        val progressRepository = mockk<EntryProgressRepository> {
            coEvery { getByEntryId(1L) } returns emptyList()
        }
        val interactions = createEntryInteractions(
            listOf(
                bookEntryInteractionPlugin(
                    BookEntryInteractionDependencies(
                        getEntryWithChapters = getEntryWithChapters,
                        entryChapterRepository = mockk(),
                        entryProgressRepository = progressRepository,
                    ),
                ),
            ),
        )
        val entry = entry()

        assertEquals(chapter, interactions.continueEntry.findNext(entry))
        assertTrue(
            interactions.consumption.canSetConsumed(
                EntryType.BOOK,
                EntryConsumptionStatus(consumed = false, bookmarked = false, hasPartialProgress = false),
                consumed = true,
            ),
        )
        assertTrue(
            interactions.consumption.canSetConsumed(
                EntryType.BOOK,
                EntryConsumptionStatus(consumed = false, bookmarked = false, hasPartialProgress = true),
                consumed = false,
            ),
        )
        assertFalse(interactions.consumption.supportsBookmark(EntryType.BOOK))
        assertFalse(interactions.download.supportsDownloads(EntryType.BOOK))
        assertFalse(interactions.capability.supportsMigration(entry))
        assertFalse(interactions.capability.supportsMerge(entry))
    }

    @Test
    fun `mark consumed updates the child without resolving source media`() = runTest {
        val chapter = chapter()
        val progressRepository = mockk<EntryProgressRepository> {
            coEvery { getByEntryId(chapter.entryId) } returns emptyList()
        }
        val captured = slot<List<EntryChapter>>()
        val chapterRepository = mockk<EntryChapterRepository> {
            coEvery { updateAll(capture(captured)) } returns true
        }

        val processor = BookConsumptionProcessor(
            entryProgressRepository = progressRepository,
            entryChapterRepository = chapterRepository,
            now = { 100L },
        )

        processor.setConsumed(entry(), listOf(chapter), consumed = true)

        assertTrue(captured.captured.single().read)
        coVerify(exactly = 0) { progressRepository.mergeAndSyncChild(any()) }
    }

    @Test
    fun `mark consumed deletes its download when preference is enabled`() = runTest {
        val entry = entry()
        val chapter = chapter()
        val progressRepository = mockk<EntryProgressRepository> {
            coEvery { getByEntryId(chapter.entryId) } returns emptyList()
        }
        val chapterRepository = mockk<EntryChapterRepository> {
            coEvery { updateAll(any()) } returns true
        }
        val downloadCleanup = mockk<BookDownloadCleanup>(relaxed = true)
        val processor = BookConsumptionProcessor(
            entryProgressRepository = progressRepository,
            entryChapterRepository = chapterRepository,
            downloadCleanup = downloadCleanup,
        )

        processor.setConsumed(entry, listOf(chapter), consumed = true)

        coVerify(exactly = 1) { downloadCleanup.afterMarkedConsumed(entry, listOf(chapter)) }
    }

    @Test
    fun `mark unread resets partial book locator`() = runTest {
        val chapter = chapter(read = false)
        val locator = EntryProgressLocator(
            kind = BOOK_PROGRESS_LOCATOR_KIND,
            progression = 0.4,
            totalProgression = 0.2,
        )
        val current = EntryProgressState(
            entryId = 1L,
            chapterId = chapter.id,
            contentKey = "volume-1",
            resourceKey = "chapter-1",
            locator = locator,
            completed = false,
            locatorUpdatedAt = 50L,
            completionUpdatedAt = 60L,
        )
        val progressRepository = mockk<EntryProgressRepository>()
        val captured = slot<EntryProgressState>()
        coEvery { progressRepository.getByEntryId(chapter.entryId) } returns listOf(current)
        coEvery { progressRepository.mergeAndSyncChild(capture(captured)) } answers { captured.captured }
        val updatedChapters = slot<List<EntryChapter>>()
        val chapterRepository = mockk<EntryChapterRepository> {
            coEvery { updateAll(capture(updatedChapters)) } returns true
        }
        val processor = BookConsumptionProcessor(
            entryProgressRepository = progressRepository,
            entryChapterRepository = chapterRepository,
            now = { 100L },
        )

        processor.setConsumed(entry(), listOf(chapter), consumed = false)

        assertFalse(captured.captured.completed)
        assertEquals(EntryProgressLocator(kind = BOOK_PROGRESS_LOCATOR_KIND), captured.captured.locator)
        assertEquals(100L, captured.captured.locatorUpdatedAt)
        assertEquals(100L, captured.captured.completionUpdatedAt)
        assertFalse(updatedChapters.captured.single().read)
    }

    @Test
    fun `mark unread resets completed book locator`() = runTest {
        val chapter = chapter(read = true)
        val current = EntryProgressState(
            entryId = 1L,
            chapterId = chapter.id,
            contentKey = "volume-1",
            resourceKey = "chapter-1",
            locator = EntryProgressLocator(
                kind = BOOK_PROGRESS_LOCATOR_KIND,
                progression = 0.4,
                totalProgression = 0.2,
            ),
            completed = true,
            locatorUpdatedAt = 50L,
            completionUpdatedAt = 60L,
        )
        val progressRepository = mockk<EntryProgressRepository>()
        val captured = slot<EntryProgressState>()
        coEvery { progressRepository.getByEntryId(chapter.entryId) } returns listOf(current)
        coEvery { progressRepository.mergeAndSyncChild(capture(captured)) } answers { captured.captured }
        val chapterRepository = mockk<EntryChapterRepository> {
            coEvery { updateAll(any()) } returns true
        }
        val processor = BookConsumptionProcessor(
            entryProgressRepository = progressRepository,
            entryChapterRepository = chapterRepository,
            now = { 100L },
        )

        processor.setConsumed(entry(), listOf(chapter), consumed = false)

        assertFalse(captured.captured.completed)
        assertEquals(EntryProgressLocator(kind = BOOK_PROGRESS_LOCATOR_KIND), captured.captured.locator)
        assertEquals(100L, captured.captured.locatorUpdatedAt)
        assertEquals(100L, captured.captured.completionUpdatedAt)
    }

    @Test
    fun `progress snapshot keeps source child key separate from resource identity`() = runTest {
        val entry = entry()
        val chapter = chapter(url = "/source/chapter-1")
        val state = EntryProgressState(
            entryId = entry.id,
            chapterId = chapter.id,
            contentKey = "volume-1",
            resourceKey = "resource-1",
            locator = EntryProgressLocator(kind = BOOK_PROGRESS_LOCATOR_KIND, progression = 0.5),
        )
        val progressRepository = mockk<EntryProgressRepository> {
            coEvery { getByEntryId(entry.id) } returns listOf(state)
        }
        val chapterRepository = mockk<EntryChapterRepository> {
            coEvery { getChapterById(chapter.id) } returns chapter
        }
        val processor = BookProgressProcessor(progressRepository, chapterRepository)

        val snapshot = processor.snapshot(entry)

        assertEquals("resource-1", snapshot.states.single().resourceKey)
        assertEquals("/source/chapter-1", snapshot.states.single().sourceChildKey)
    }

    @Test
    fun `reader host reports the unsupported descriptor when no processor is registered`() = runTest {
        val entry = entry()
        val chapter = chapter()
        val source = mockk<UnifiedSource>()
        val descriptor = BookContentDescriptor(
            format = "application/epub+zip",
            profile = "fixed-layout",
            protection = "none",
        )
        val coordinator = BookProcessorSelectionCoordinator(
            registry = BookProcessorRegistry(emptyList()),
            preferences = BookProcessorPreferences(InMemoryPreferenceStore()),
        )
        val sessionFactory = mockk<BookReaderSessionFactory>()
        coEvery { sessionFactory.prepare(BookReaderRequest(entry.id, chapter.id)) } returns
            BookReaderPrepareResult.Success(
                PreparedBookReaderRequest(
                    request = BookReaderRequest(entry.id, chapter.id),
                    visibleEntry = entry,
                    owner = entry,
                    chapter = chapter,
                    content = PreparedBookContent.Source(source, EntryMedia.Book(descriptor)),
                ),
            )
        val resolver = BookReaderHostResolver(sessionFactory, coordinator)

        val state = resolver.resolve(entry.id, chapter.id) as BookReaderHostState.Unavailable

        assertEquals(BookFailureReason.PROCESSOR_UNAVAILABLE, state.failure.reason)
        assertEquals(descriptor, state.descriptor)
        assertTrue(state.failure.message.contains(descriptor.format))
        assertTrue(state.failure.message.contains("fixed-layout"))
    }

    private fun entry(): Entry = Entry.create().copy(
        id = 1L,
        source = 9L,
        url = "/book",
        title = "Book",
        type = EntryType.BOOK,
    )

    private fun chapter(
        id: Long = 10L,
        read: Boolean = false,
        url: String = "/chapter/1",
        chapterNumber: Double = 1.0,
    ): EntryChapter = EntryChapter.create().copy(
        id = id,
        entryId = 1L,
        url = url,
        name = "Chapter 1",
        read = read,
        chapterNumber = chapterNumber,
    )
}
