package mihon.entry.interactions.book

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.source.entry.BookResourceCatalog
import eu.kanade.tachiyomi.source.entry.BookResourceLocation
import eu.kanade.tachiyomi.source.entry.BookSourceResource
import eu.kanade.tachiyomi.source.entry.EntryMedia
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import mihon.book.api.BookContentDescriptor
import mihon.book.api.BookLocator
import mihon.book.api.BookPublication
import mihon.entry.interactions.book.download.BookDownloadCache
import okhttp3.OkHttpClient
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.model.EntryProgressState
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.entry.repository.EntryProgressRepository
import tachiyomi.domain.entry.repository.EntryRepository
import tachiyomi.domain.history.model.HistoryUpdate
import tachiyomi.domain.history.repository.HistoryRepository
import tachiyomi.domain.source.service.SourceManager
import java.nio.file.Files
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class BookReaderSessionFactoryTest {
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
            locator = BookProgressLocatorCodec.encode(initialLocator),
            completed = false,
            completionUpdatedAt = 0L,
        )
        val updatedProgress = slot<EntryProgressState>()
        val progressRepository = mockk<EntryProgressRepository> {
            coEvery { get(entry.id, "volume-1", "publication.epub") } returns currentProgress
            coEvery { mergeAndSyncChild(capture(updatedProgress)) } answers { updatedProgress.captured }
        }
        val historyRepository = mockk<HistoryRepository>(relaxed = true)
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
        val publicationSession = TestPublicationSession()
        val processor = SessionFactoryTestProcessor(publicationSession)
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
            historyRepository = historyRepository,
            sourceManager = mockk<SourceManager> {
                every { get(entry.source) } returns source
            },
            processorRegistry = BookProcessorRegistry(listOf(processor)),
            networkHelper = mockk<NetworkHelper> {
                every { client } returns mockk<OkHttpClient>()
            },
            incognitoState = mockk {
                every { isIncognito(entry.source) } returns false
            },
            materializationStore = mockk(relaxed = true),
            downloadCache = failingDownloadCache(),
            now = { 100L },
        )

        val prepared = assertIs<BookReaderPrepareResult.Success>(
            factory.prepare(BookReaderRequest(entry.id, chapter.id)),
        )
        val result = assertIs<BookReaderOpenResult.Success>(
            factory.openPrepared(context, prepared.request, processor.id),
        )
        val session = result.session

        coVerify(exactly = 1) { source.getMedia(any(), any()) }

        assertEquals(initialLocator, session.initialLocator)
        val latestLocator = BookLocator("chapter-2.xhtml", progression = 0.5, totalProgression = 0.6)
        session.saveLocation(latestLocator, completed = true)
        assertEquals(latestLocator, BookProgressLocatorCodec.decode(updatedProgress.captured.locator))
        assertTrue(updatedProgress.captured.completed)
        assertEquals(100L, updatedProgress.captured.completionUpdatedAt)
        assertEquals(100L, updatedProgress.captured.locatorUpdatedAt)

        session.recordHistory(500L)
        coVerify {
            historyRepository.upsertHistory(
                match<HistoryUpdate> {
                    it.chapterId == chapter.id && it.readAt.time == 100L && it.sessionReadDuration == 500L
                },
            )
        }

        session.close()
        assertEquals(1, publicationSession.closeCount)
        assertTrue(checkNotNull(processor.contentSession).getResource("publication.epub").isFailure)
    }

    @Test
    fun `saving progress preserves a manually consumed child without resolving media`() = runTest {
        val chapter = chapter().copy(read = true)
        val captured = slot<EntryProgressState>()
        val progressRepository = mockk<EntryProgressRepository> {
            coEvery { get(chapter.entryId, "", "publication.epub") } returns null
            coEvery { mergeAndSyncChild(capture(captured)) } answers { captured.captured }
        }
        val session = OpenedBookReaderSession(
            entry = entry(),
            historySourceId = entry().source,
            chapter = chapter,
            progressIdentity = BookProgressIdentity("", "publication.epub", null),
            contentSession = mockk(relaxed = true),
            publicationSession = TestPublicationSession(),
            initialLocator = null,
            entryProgressRepository = progressRepository,
            historyRepository = mockk(relaxed = true),
            incognitoState = mockk {
                every { isIncognito(entry().source) } returns false
            },
            now = { 100L },
        )

        session.saveLocation(BookLocator("chapter-1.xhtml", progression = 0.1))

        assertTrue(captured.captured.completed)
        assertEquals(100L, captured.captured.completionUpdatedAt)
        session.close()
    }

    @Test
    fun `merged book history uses the child owner source for incognito`() = runTest {
        val owner = entry()
        val visible = entry().copy(id = 2L, source = 20L, url = "/merged")
        val chapter = chapter()
        val source = mockk<UnifiedSource> {
            every { id } returns owner.source
            coEvery { getMedia(any(), any()) } returns EntryMedia.Book(
                descriptor = BookContentDescriptor("application/epub+zip"),
                catalog = BookResourceCatalog(
                    resources = listOf(
                        BookSourceResource(
                            id = "publication.epub",
                            location = BookResourceLocation.InlineBytes(byteArrayOf(1)),
                        ),
                    ),
                ),
                initialResourceId = "publication.epub",
            )
        }
        val progressRepository = mockk<EntryProgressRepository> {
            coEvery { get(owner.id, any(), any()) } returns null
        }
        val historyRepository = mockk<HistoryRepository>(relaxed = true)
        val incognitoState = mockk<mihon.entry.interactions.EntryReaderIncognitoState> {
            every { isIncognito(owner.source) } returns true
        }
        val processor = SessionFactoryTestProcessor(TestPublicationSession())
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
            historyRepository = historyRepository,
            sourceManager = mockk {
                every { get(owner.source) } returns source
            },
            processorRegistry = BookProcessorRegistry(listOf(processor)),
            networkHelper = mockk {
                every { client } returns mockk<OkHttpClient>()
            },
            incognitoState = incognitoState,
            materializationStore = mockk(relaxed = true),
            downloadCache = emptyDownloadCache(),
        )

        val session = assertIs<BookReaderOpenResult.Success>(
            factory.open(context, BookReaderRequest(visible.id, chapter.id), processor.id),
        ).session
        session.saveLocation(BookLocator("chapter-1.xhtml", progression = 0.5))
        session.recordHistory(500L)

        coVerify { incognitoState.isIncognito(owner.source) }
        coVerify(exactly = 1) { progressRepository.get(owner.id, any(), any()) }
        coVerify(exactly = 0) { progressRepository.mergeAndSyncChild(any()) }
        coVerify(exactly = 0) { historyRepository.upsertHistory(any()) }
        session.close()
    }

    private fun entry(): Entry = Entry.create().copy(
        id = 1L,
        source = 9L,
        url = "/book",
        title = "Book",
        type = EntryType.BOOK,
    )

    private fun chapter(): EntryChapter = EntryChapter.create().copy(
        id = 10L,
        entryId = 1L,
        url = "/publication.epub",
        name = "EPUB",
    )

    private fun emptyDownloadCache(): BookDownloadCache {
        val cache = mockk<BookDownloadCache>()
        coEvery { cache.ensureInitialized() } returns Unit
        every { cache.get(any()) } returns null
        return cache
    }

    private fun failingDownloadCache(): BookDownloadCache = mockk {
        coEvery { ensureInitialized() } throws java.io.IOException("storage unavailable")
    }
}

private class SessionFactoryTestProcessor(
    private val publicationSession: BookPublicationSession,
) : BookProcessor {
    override val id = "test.epub"
    override val displayName = "Test EPUB"
    var contentSession: BookContentSession? = null

    override fun supports(descriptor: BookContentDescriptor): Boolean =
        descriptor.format == "application/epub+zip"

    override fun createReaderIntent(
        context: Context,
        request: BookReaderRequest,
        sessionToken: String,
    ): Intent = Intent()

    override suspend fun open(content: BookContentSession): BookOpenResult {
        contentSession = content
        return BookOpenResult.Success(publicationSession)
    }
}

private class TestPublicationSession : BookPublicationSession {
    override val publication = BookPublication(
        id = "book",
        revision = "1",
        title = "Book",
        languages = emptyList(),
        readingDirection = null,
        readingOrder = emptyList(),
        navigation = emptyList(),
    )
    var closeCount = 0

    override fun validate(locator: BookLocator): Boolean = true

    override fun close() {
        closeCount++
    }
}
