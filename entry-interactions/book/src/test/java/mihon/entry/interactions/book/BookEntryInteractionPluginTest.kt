package mihon.entry.interactions.book

import eu.kanade.tachiyomi.source.entry.BookResourceCatalog
import eu.kanade.tachiyomi.source.entry.BookSourceResource
import eu.kanade.tachiyomi.source.entry.EntryMedia
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import mihon.book.api.BookContentDescriptor
import mihon.book.api.BookFailureReason
import mihon.entry.interactions.EntryConsumptionStatus
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
import tachiyomi.domain.source.service.SourceManager
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BookEntryInteractionPluginTest {
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
                        sourceManager = mockk(),
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
        assertFalse(interactions.consumption.supportsBookmark(EntryType.BOOK))
        assertFalse(interactions.download.supportsDownloads(EntryType.BOOK))
        assertFalse(interactions.capability.supportsMigration(entry))
        assertFalse(interactions.capability.supportsMerge(entry))
    }

    @Test
    fun `mark consumed persists source publication and resource identity`() = runTest {
        val chapter = chapter()
        val progressRepository = mockk<EntryProgressRepository>()
        val captured = slot<EntryProgressState>()
        coEvery { progressRepository.get(1L, "volume-1", "epub") } returns null
        coEvery { progressRepository.mergeAndSyncChild(capture(captured)) } answers { captured.captured }

        val processor = BookConsumptionProcessor(
            entryProgressRepository = progressRepository,
            identityResolver = identityResolver(
                chapter = chapter,
                media = EntryMedia.Book(
                    descriptor = BookContentDescriptor("application/epub+zip"),
                    publicationKeyOverride = "volume-1",
                    publicationRevision = "publication-2",
                    catalog = BookResourceCatalog(
                        resources = listOf(BookSourceResource(id = "epub", revision = "resource-3")),
                    ),
                    initialResourceId = "epub",
                ),
            ),
            now = { 100L },
        )

        processor.setConsumed(entry(), listOf(chapter), consumed = true)

        assertEquals(1L, captured.captured.entryId)
        assertEquals(chapter.id, captured.captured.chapterId)
        assertEquals("volume-1", captured.captured.contentKey)
        assertEquals("epub", captured.captured.resourceKey)
        assertEquals("resource-3", captured.captured.resourceRevision)
        assertEquals(BOOK_PROGRESS_LOCATOR_KIND, captured.captured.locator.kind)
        assertTrue(captured.captured.completed)
        assertEquals(100L, captured.captured.completionUpdatedAt)
    }

    @Test
    fun `mark unread retains precise book locator`() = runTest {
        val chapter = chapter(read = true)
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
            completed = true,
            locatorUpdatedAt = 50L,
            completionUpdatedAt = 60L,
        )
        val progressRepository = mockk<EntryProgressRepository>()
        val captured = slot<EntryProgressState>()
        coEvery { progressRepository.get(1L, "volume-1", "chapter-1") } returns current
        coEvery { progressRepository.mergeAndSyncChild(capture(captured)) } answers { captured.captured }
        val processor = BookConsumptionProcessor(
            entryProgressRepository = progressRepository,
            identityResolver = identityResolver(
                chapter = chapter,
                media = EntryMedia.Book(
                    descriptor = BookContentDescriptor("text/html", profile = "web-novel"),
                    publicationKeyOverride = "volume-1",
                    initialResourceId = "chapter-1",
                ),
            ),
            now = { 100L },
        )

        processor.setConsumed(entry(), listOf(chapter), consumed = false)

        assertFalse(captured.captured.completed)
        assertEquals(locator, captured.captured.locator)
        assertEquals(50L, captured.captured.locatorUpdatedAt)
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
                    source = source,
                    media = EntryMedia.Book(descriptor),
                ),
            )
        val resolver = BookReaderHostResolver(sessionFactory, coordinator)

        val state = resolver.resolve(entry.id, chapter.id) as BookReaderHostState.Unavailable

        assertEquals(BookFailureReason.PROCESSOR_UNAVAILABLE, state.failure.reason)
        assertEquals(descriptor, state.descriptor)
        assertTrue(state.failure.message.contains(descriptor.format))
        assertTrue(state.failure.message.contains("fixed-layout"))
    }

    private fun identityResolver(
        chapter: EntryChapter,
        media: EntryMedia.Book,
    ): BookProgressIdentityResolver {
        val owner = entry()
        val source = mockk<UnifiedSource>()
        val getEntryWithChapters = mockk<GetEntryWithChapters> {
            coEvery { awaitEntry(chapter.entryId) } returns owner
        }
        val sourceManager = mockk<SourceManager> {
            every { get(owner.source) } returns source
        }
        coEvery { source.getMedia(any(), any()) } returns media
        return BookProgressIdentityResolver(getEntryWithChapters, sourceManager)
    }

    private fun entry(): Entry = Entry.create().copy(
        id = 1L,
        source = 9L,
        url = "/book",
        title = "Book",
        type = EntryType.BOOK,
    )

    private fun chapter(
        read: Boolean = false,
        url: String = "/chapter/1",
    ): EntryChapter = EntryChapter.create().copy(
        id = 10L,
        entryId = 1L,
        url = url,
        name = "Chapter 1",
        read = read,
        chapterNumber = 1.0,
    )
}
