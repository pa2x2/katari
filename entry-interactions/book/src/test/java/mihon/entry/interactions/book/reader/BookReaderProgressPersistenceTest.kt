package mihon.entry.interactions.book.reader

import android.content.ContentResolver
import android.content.Context
import eu.kanade.tachiyomi.source.entry.BookResourceCatalog
import eu.kanade.tachiyomi.source.entry.BookResourceLocation
import eu.kanade.tachiyomi.source.entry.BookSourceResource
import eu.kanade.tachiyomi.source.entry.EntryMedia
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import mihon.book.api.BookContentDescriptor
import mihon.book.api.BookLocator
import mihon.entry.interactions.book.media.session.BookMediaSessionProcessor
import mihon.entry.interactions.book.migration.BOOK_PENDING_MIGRATION_CONTENT_KEY
import mihon.entry.interactions.book.migration.bookPendingMigrationResourceKey
import mihon.entry.interactions.book.preparation.BookContentPreparerRegistry
import mihon.entry.interactions.book.processor.BookReaderProcessorRegistry
import mihon.entry.interactions.book.processor.BookReaderRequest
import mihon.entry.interactions.book.state.BookProgressIdentity
import mihon.entry.interactions.book.state.BookProgressLocatorCodec
import mihon.entry.interactions.media.session.EntryMediaSessionEvent
import mihon.entry.interactions.media.session.EntryMediaSessionEventSink
import mihon.entry.interactions.media.session.EntryMediaSessionResult
import okhttp3.OkHttpClient
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.EntryProgressState
import tachiyomi.domain.entry.repository.EntryProgressRepository
import java.nio.file.Files
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal class BookReaderProgressPersistenceTest : BookReaderSessionFixture() {
    @Test
    fun `completed book progress reopens from the beginning`() = runTest {
        val locator = BookLocator("chapter-1.xhtml", progression = 0.8)
        val session = openWithProgress(
            chapter = chapter(),
            progress = bookProgress(locator, completed = true),
        )

        assertNull(session.initialLocator)
        session.close()
    }

    @Test
    fun `read child ignores an inconsistent incomplete locator`() = runTest {
        val locator = BookLocator("chapter-1.xhtml", progression = 0.4)
        val session = openWithProgress(
            chapter = chapter().copy(read = true),
            progress = bookProgress(locator, completed = false),
        )

        assertNull(session.initialLocator)
        session.close()
    }

    @Test
    fun `current saved locator is reconciled against the prepared model`() = runTest {
        val sourceLocator = BookLocator("old-chapter.xhtml", progression = 0.4, totalProgression = 0.3)
        val targetLocator = BookLocator("new-chapter.xhtml", progression = 0.45, totalProgression = 0.3)
        val session = openWithProgress(
            chapter = chapter(),
            progress = bookProgress(sourceLocator, completed = false),
            preparedPublication = MigratingPublicationSession(targetLocator),
        )

        assertEquals(targetLocator, session.initialLocator)
        session.close()
    }

    @Test
    fun `failed saved locator reconciliation does not prevent content from opening`() = runTest {
        val sourceLocator = BookLocator("old-chapter.xhtml", progression = 0.4)
        val session = openWithProgress(
            chapter = chapter(),
            progress = bookProgress(sourceLocator, completed = false),
            preparedPublication = LocatorRestorationPublicationSession("chapter.xhtml") {
                error("reconciliation unavailable")
            },
        )

        assertNull(session.initialLocator)
        session.close()
    }

    @Test
    fun `invalid reconciled saved locator is discarded`() = runTest {
        val sourceLocator = BookLocator("old-chapter.xhtml", progression = 0.4)
        val session = openWithProgress(
            chapter = chapter(),
            progress = bookProgress(sourceLocator, completed = false),
            preparedPublication = LocatorRestorationPublicationSession("chapter.xhtml") {
                BookLocator("still-stale.xhtml", progression = 0.4)
            },
        )

        assertNull(session.initialLocator)
        session.close()
    }

    @Test
    fun `first target open reconciles and rekeys pending migrated progress`() = runTest {
        val entry = entry()
        val chapter = chapter()
        val sourceLocator = BookLocator("source-chapter.xhtml", progression = 0.25, totalProgression = 0.2)
        val targetLocator = BookLocator("target-chapter.xhtml", progression = 0.3, totalProgression = 0.2)
        val pending = EntryProgressState(
            entryId = entry.id,
            chapterId = chapter.id,
            contentKey = BOOK_PENDING_MIGRATION_CONTENT_KEY,
            resourceKey = bookPendingMigrationResourceKey(chapter.id),
            locator = BookProgressLocatorCodec.encode(sourceLocator),
            locatorUpdatedAt = 50L,
        )
        val resolved = pending.copy(
            contentKey = "volume-1",
            resourceKey = "chapter.html",
            locator = BookProgressLocatorCodec.encode(targetLocator),
        )
        val promoted = slot<EntryProgressState>()
        val progressRepository = mockk<EntryProgressRepository> {
            coEvery { get(entry.id, "volume-1", "chapter.html") } returnsMany listOf(null, resolved)
            coEvery { getByEntryId(entry.id) } returns listOf(pending)
            coEvery { upsert(capture(promoted)) } returns Unit
            coEvery { rekey(any(), any(), any(), any(), any(), any()) } returns Unit
        }
        val source = mockk<UnifiedSource> {
            every { id } returns entry.source
            coEvery { getMedia(any(), any()) } returns EntryMedia.Book(
                descriptor = BookContentDescriptor("text/html"),
                publicationKeyOverride = "volume-1",
                catalog = BookResourceCatalog(
                    resources = listOf(
                        BookSourceResource(
                            id = "chapter.html",
                            location = BookResourceLocation.InlineBytes(byteArrayOf(1, 2, 3)),
                        ),
                    ),
                ),
                initialResourceId = "chapter.html",
            )
        }
        val preparedPublication = MigratingPublicationSession(targetLocator)
        val preparer = SessionFactoryTestPreparer(preparedPublication)
        val reader = SessionFactoryTestReaderProcessor()
        val context = mockk<Context> {
            every { applicationContext } returns this@mockk
            every { contentResolver } returns mockk<ContentResolver>()
            every { cacheDir } returns Files.createTempDirectory("book-reader-migration").toFile()
        }
        val factory = BookReaderSessionFactory(
            entryRepository = mockk {
                coEvery { getEntryById(entry.id) } returns entry
            },
            entryChapterRepository = mockk {
                coEvery { getChapterById(chapter.id) } returns chapter
            },
            entryProgressRepository = progressRepository,
            sourceManager = mockk {
                every { get(entry.source) } returns source
            },
            preparerRegistry = BookContentPreparerRegistry(listOf(preparer)),
            readerProcessorRegistry = BookReaderProcessorRegistry(listOf(reader)),
            networkHelper = mockk {
                every { client } returns mockk<OkHttpClient>()
            },
            materializationStore = mockk(relaxed = true),
            downloadCache = emptyDownloadCache(),
            mediaSession = mockk(relaxed = true),
        )

        val session = assertIs<BookReaderOpenResult.Success>(
            factory.open(context, BookReaderRequest(entry.id, chapter.id), reader.id),
        ).session

        assertEquals(targetLocator, session.initialLocator)
        assertEquals(targetLocator, BookProgressLocatorCodec.decode(promoted.captured.locator))
        coVerify(exactly = 1) {
            progressRepository.rekey(
                entry.id,
                chapter.id,
                BOOK_PENDING_MIGRATION_CONTENT_KEY,
                bookPendingMigrationResourceKey(chapter.id),
                "volume-1",
                "chapter.html",
            )
        }
        session.close()
    }

    @Test
    fun `saving progress preserves a manually consumed child without resolving media`() = runTest {
        val chapter = chapter().copy(read = true)
        val events = mutableListOf<EntryMediaSessionEvent>()
        val session = OpenedBookReaderSession(
            entry = entry(),
            owner = entry(),
            chapter = chapter,
            progressIdentity = BookProgressIdentity("", "chapter.html", null),
            contentSession = mockk(relaxed = true),
            preparedPublication = TestPublicationSession(),
            initialLocator = null,
            mediaSession = BookMediaSessionProcessor(
                EntryMediaSessionEventSink {
                    events += it
                    EntryMediaSessionResult.Handled
                },
            ),
            now = { 100L },
        )

        session.saveLocation(BookLocator("chapter-1.xhtml", progression = 0.1))

        val event = assertIs<EntryMediaSessionEvent.Progressed>(events.single())
        assertTrue(event.progress.completed)
        assertEquals(100L, event.progress.completionUpdatedAt)
        session.close()
    }
}
