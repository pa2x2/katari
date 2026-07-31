package mihon.entry.interactions.book.reader

import android.content.ContentResolver
import android.content.Context
import eu.kanade.tachiyomi.source.entry.BookResourceCatalog
import eu.kanade.tachiyomi.source.entry.BookResourceLocation
import eu.kanade.tachiyomi.source.entry.BookSourceResource
import eu.kanade.tachiyomi.source.entry.EntryMedia
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import mihon.book.api.BookContentDescriptor
import mihon.book.api.BookLocator
import mihon.entry.interactions.book.media.session.BookMediaSessionProcessor
import mihon.entry.interactions.book.preparation.BookContentPreparerRegistry
import mihon.entry.interactions.book.processor.BookReaderProcessorRegistry
import mihon.entry.interactions.book.processor.BookReaderRequest
import mihon.entry.interactions.media.session.EntryMediaSessionEvent
import mihon.entry.interactions.media.session.EntryMediaSessionEventSink
import mihon.entry.interactions.media.session.EntryMediaSessionResult
import okhttp3.OkHttpClient
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.repository.EntryProgressRepository
import java.nio.file.Files
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

internal class BookReaderOwnerPolicyTest : BookReaderSessionFixture() {
    @Test
    fun `merged book reports visible entry and owner child to shared policy`() = runTest {
        val owner = entry()
        val visible = entry().copy(id = 2L, source = 20L, url = "/merged")
        val chapter = chapter()
        val source = mockk<UnifiedSource> {
            every { id } returns owner.source
            coEvery { getMedia(any(), any()) } returns EntryMedia.Book(
                descriptor = BookContentDescriptor("text/html"),
                catalog = BookResourceCatalog(
                    resources = listOf(
                        BookSourceResource(
                            id = "chapter.html",
                            location = BookResourceLocation.InlineBytes(byteArrayOf(1)),
                        ),
                    ),
                ),
                initialResourceId = "chapter.html",
            )
        }
        val progressRepository = mockk<EntryProgressRepository> {
            coEvery { get(owner.id, any(), any()) } returns null
            coEvery { getByEntryId(owner.id) } returns emptyList()
        }
        val events = mutableListOf<EntryMediaSessionEvent>()
        val preparer = SessionFactoryTestPreparer(TestPublicationSession())
        val reader = SessionFactoryTestReaderProcessor()
        val context = mockk<Context> {
            every { applicationContext } returns this@mockk
            every { contentResolver } returns mockk<ContentResolver>()
            every { cacheDir } returns Files.createTempDirectory("book-reader-session-merged").toFile()
        }
        val factory = BookReaderSessionFactory(
            entryRepository = mockk {
                coEvery { getEntryById(visible.id) } returns visible
                coEvery { getEntryById(owner.id) } returns owner
            },
            entryChapterRepository = mockk {
                coEvery { getChapterById(chapter.id) } returns chapter
            },
            entryProgressRepository = progressRepository,
            sourceManager = mockk {
                every { get(owner.source) } returns source
            },
            preparerRegistry = BookContentPreparerRegistry(listOf(preparer)),
            readerProcessorRegistry = BookReaderProcessorRegistry(listOf(reader)),
            networkHelper = mockk {
                every { client } returns mockk<OkHttpClient>()
            },
            materializationStore = mockk(relaxed = true),
            downloadCache = emptyDownloadCache(),
            mediaSession = BookMediaSessionProcessor(
                EntryMediaSessionEventSink {
                    events += it
                    EntryMediaSessionResult.Handled
                },
            ),
        )

        val session = assertIs<BookReaderOpenResult.Success>(
            factory.open(context, BookReaderRequest(visible.id, chapter.id), reader.id),
        ).session
        assertEquals(owner, session.owner)
        session.saveLocation(BookLocator("chapter-1.xhtml", progression = 0.5))
        session.recordHistory(500L)

        assertTrue(events.all { it.visibleEntry == visible && it.child == chapter })
        session.close()
    }
}
