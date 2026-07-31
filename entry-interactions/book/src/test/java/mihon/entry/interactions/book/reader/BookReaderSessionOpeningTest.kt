package mihon.entry.interactions.book

import android.content.ContentResolver
import android.content.Context
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.source.entry.BookResourceCatalog
import eu.kanade.tachiyomi.source.entry.BookResourceLocation
import eu.kanade.tachiyomi.source.entry.BookSourceResource
import eu.kanade.tachiyomi.source.entry.EntryMedia
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import mihon.book.api.BookContentDescriptor
import mihon.book.api.BookLocator
import mihon.book.api.BookResource
import mihon.entry.interactions.EntryMediaSessionEvent
import mihon.entry.interactions.EntryMediaSessionEventSink
import mihon.entry.interactions.EntryMediaSessionResult
import okhttp3.OkHttpClient
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.EntryProgressLocator
import tachiyomi.domain.entry.model.EntryProgressState
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.entry.repository.EntryProgressRepository
import tachiyomi.domain.entry.repository.EntryRepository
import tachiyomi.domain.source.service.SourceManager
import java.nio.file.Files
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

internal class BookReaderSessionOpeningTest : BookReaderSessionFixture() {
    @Test
    fun `opens selected processor restores locator saves progress and closes in ownership order`() = runTest {
        val entry = entry()
        val chapter = chapter()
        val initialLocator = BookLocator("chapter-1.xhtml", progression = 0.25, totalProgression = 0.1)
        val currentProgress = EntryProgressState(
            entryId = entry.id,
            chapterId = chapter.id,
            contentKey = "volume-1",
            resourceKey = "publication.epub",
            locator = EntryProgressLocator(
                kind = BOOK_PROGRESS_LOCATOR_KIND,
                progression = initialLocator.progression,
                totalProgression = initialLocator.totalProgression,
            ),
            completed = false,
            completionUpdatedAt = 0L,
        )
        val progressRepository = mockk<EntryProgressRepository> {
            coEvery { get(entry.id, "volume-1", "publication.epub") } returns currentProgress
            coEvery { getByEntryId(entry.id) } returns emptyList()
        }
        val events = mutableListOf<EntryMediaSessionEvent>()
        val source = mockk<UnifiedSource> {
            every { id } returns entry.source
        }
        coEvery { source.getMedia(any(), any()) } returns EntryMedia.Book(
            descriptor = BookContentDescriptor("application/epub+zip"),
            publicationKeyOverride = "volume-1",
            catalog = BookResourceCatalog(
                resources = listOf(
                    BookSourceResource(
                        id = "publication.epub",
                        location = BookResourceLocation.InlineBytes(byteArrayOf(1, 2, 3)),
                    ),
                ),
            ),
            initialResourceId = "publication.epub",
        )
        val preparedPublication = TestPublicationSession(
            readingOrder = listOf(
                BookResource(
                    id = initialLocator.resourceId,
                    mediaType = "application/xhtml+xml",
                    title = null,
                ),
            ),
        )
        val preparer = SessionFactoryTestPreparer(preparedPublication)
        val reader = SessionFactoryTestReaderProcessor()
        val context = mockk<Context> {
            every { applicationContext } returns this@mockk
            every { contentResolver } returns mockk<ContentResolver>()
            every { cacheDir } returns Files.createTempDirectory("book-reader-session").toFile()
        }
        val factory = BookReaderSessionFactory(
            entryRepository = mockk<EntryRepository> {
                coEvery { getEntryById(entry.id) } returns entry
            },
            entryChapterRepository = mockk<EntryChapterRepository> {
                coEvery { getChapterById(chapter.id) } returns chapter
            },
            entryProgressRepository = progressRepository,
            sourceManager = mockk<SourceManager> {
                every { get(entry.source) } returns source
            },
            preparerRegistry = BookContentPreparerRegistry(listOf(preparer)),
            readerProcessorRegistry = BookReaderProcessorRegistry(listOf(reader)),
            networkHelper = mockk<NetworkHelper> {
                every { client } returns mockk<OkHttpClient>()
            },
            materializationStore = mockk(relaxed = true),
            downloadCache = failingDownloadCache(),
            mediaSession = BookMediaSessionProcessor(
                EntryMediaSessionEventSink {
                    events += it
                    EntryMediaSessionResult.Handled
                },
            ),
            now = { 100L },
        )

        val prepared = assertIs<BookReaderPrepareResult.Success>(
            factory.prepare(BookReaderRequest(entry.id, chapter.id)),
        )
        val result = assertIs<BookReaderOpenResult.Success>(
            factory.openPrepared(context, prepared.request, reader.id),
        )
        val session = result.session

        coVerify(exactly = 1) { source.getMedia(any(), any()) }

        assertEquals(preparedPublication.model, reader.receivedModel)
        assertEquals(initialLocator, session.initialLocator)
        val latestLocator = BookLocator("chapter-2.xhtml", progression = 0.5, totalProgression = 0.6)
        session.saveLocation(latestLocator, completed = true)
        val progressEvent = assertIs<EntryMediaSessionEvent.Progressed>(events.single())
        assertEquals(latestLocator, BookProgressLocatorCodec.decode(progressEvent.progress.locator))
        assertTrue(progressEvent.progress.completed)
        assertEquals(100L, progressEvent.progress.completionUpdatedAt)
        assertEquals(100L, progressEvent.progress.locatorUpdatedAt)

        session.recordHistory(500L)
        val activityEvent = assertIs<EntryMediaSessionEvent.ActivityRecorded>(events.last())
        assertEquals(100L, activityEvent.activity.recordedAtEpochMillis)
        assertEquals(500L, activityEvent.activity.durationMillis)

        session.close()
        assertEquals(1, preparedPublication.closeCount)
        assertTrue(checkNotNull(preparer.contentSession).getResource("publication.epub").isFailure)
    }
}
