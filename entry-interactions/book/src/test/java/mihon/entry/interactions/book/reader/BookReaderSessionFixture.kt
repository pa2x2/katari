package mihon.entry.interactions.book

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import eu.kanade.tachiyomi.source.entry.BookResourceCatalog
import eu.kanade.tachiyomi.source.entry.BookResourceLocation
import eu.kanade.tachiyomi.source.entry.BookSourceResource
import eu.kanade.tachiyomi.source.entry.EntryMedia
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import mihon.book.api.BookContentDescriptor
import mihon.book.api.BookLocator
import mihon.book.api.BookPublication
import mihon.book.api.BookResource
import mihon.entry.interactions.book.download.BookDownloadCache
import okhttp3.OkHttpClient
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.model.EntryProgressState
import java.nio.file.Files
import kotlin.test.assertIs

internal abstract class BookReaderSessionFixture {
    protected fun entry(): Entry = Entry.create().copy(
        id = 1L,
        source = 9L,
        url = "/book",
        title = "Book",
        type = EntryType.BOOK,
    )

    protected fun chapter(): EntryChapter = EntryChapter.create().copy(
        id = 10L,
        entryId = 1L,
        url = "/publication.epub",
        name = "EPUB",
    )

    protected fun bookProgress(
        locator: BookLocator,
        completed: Boolean,
    ) = EntryProgressState(
        entryId = entry().id,
        chapterId = chapter().id,
        contentKey = "volume-1",
        resourceKey = "publication.epub",
        locator = BookProgressLocatorCodec.encode(locator),
        completed = completed,
    )

    protected suspend fun openWithProgress(
        chapter: EntryChapter,
        progress: EntryProgressState,
    ): OpenedBookReaderSession {
        val entry = entry()
        val source = mockk<UnifiedSource> {
            every { id } returns entry.source
            coEvery { getMedia(any(), any()) } returns EntryMedia.Book(
                descriptor = BookContentDescriptor("application/epub+zip"),
                publicationKeyOverride = "volume-1",
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
        val processor = SessionFactoryTestProcessor(
            TestPublicationSession(
                readingOrder = listOf(
                    BookResource(
                        id = "chapter-1.xhtml",
                        mediaType = "application/xhtml+xml",
                        title = null,
                    ),
                ),
            ),
        )
        val context = mockk<Context> {
            every { applicationContext } returns this@mockk
            every { contentResolver } returns mockk<ContentResolver>()
            every { cacheDir } returns Files.createTempDirectory("book-reader-completed").toFile()
        }
        val factory = BookReaderSessionFactory(
            entryRepository = mockk {
                coEvery { getEntryById(entry.id) } returns entry
            },
            entryChapterRepository = mockk {
                coEvery { getChapterById(chapter.id) } returns chapter
            },
            entryProgressRepository = mockk {
                coEvery { get(entry.id, "volume-1", "publication.epub") } returns progress
                coEvery { getByEntryId(entry.id) } returns emptyList()
            },
            sourceManager = mockk {
                every { get(entry.source) } returns source
            },
            processorRegistry = BookProcessorRegistry(listOf(processor)),
            networkHelper = mockk {
                every { client } returns mockk<OkHttpClient>()
            },
            materializationStore = mockk(relaxed = true),
            downloadCache = emptyDownloadCache(),
            mediaSession = mockk(relaxed = true),
        )

        return assertIs<BookReaderOpenResult.Success>(
            factory.open(context, BookReaderRequest(entry.id, chapter.id), processor.id),
        ).session
    }

    protected fun emptyDownloadCache(): BookDownloadCache {
        val cache = mockk<BookDownloadCache>()
        coEvery { cache.getVerified(any()) } returns null
        return cache
    }

    protected fun failingDownloadCache(): BookDownloadCache = mockk {
        coEvery { getVerified(any()) } throws java.io.IOException("storage unavailable")
    }
}
internal class SessionFactoryTestProcessor(
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

internal class TestPublicationSession(
    readingOrder: List<BookResource> = emptyList(),
) : BookPublicationSession {
    override val publication = BookPublication(
        id = "book",
        revision = "1",
        title = "Book",
        languages = emptyList(),
        readingDirection = null,
        readingOrder = readingOrder,
        navigation = emptyList(),
    )
    var closeCount = 0

    override fun validate(locator: BookLocator): Boolean = true

    override fun close() {
        closeCount++
    }
}

internal class MigratingPublicationSession(
    private val targetLocator: BookLocator,
) : BookPublicationSession {
    override val publication = BookPublication(
        id = "target-book",
        revision = "1",
        title = "Target Book",
        languages = emptyList(),
        readingDirection = null,
        readingOrder = emptyList(),
        navigation = emptyList(),
    )

    override fun validate(locator: BookLocator): Boolean = locator.resourceId == targetLocator.resourceId

    override suspend fun reconcileMigratedLocator(locator: BookLocator): BookLocator = targetLocator

    override fun close() = Unit
}
