package mihon.entry.interactions.book.reader

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
import mihon.book.api.model.BookPublicationModel
import mihon.book.api.model.BookPublicationModelDescriptor
import mihon.entry.interactions.book.content.BookContentSession
import mihon.entry.interactions.book.download.BookDownloadCache
import mihon.entry.interactions.book.preparation.BookContentPreparer
import mihon.entry.interactions.book.preparation.BookContentPreparerRegistry
import mihon.entry.interactions.book.preparation.BookPreparationResult
import mihon.entry.interactions.book.preparation.BookPublicationResource
import mihon.entry.interactions.book.preparation.BookPublicationResourceLoader
import mihon.entry.interactions.book.preparation.PreparedBookPublication
import mihon.entry.interactions.book.processor.BookReaderProcessor
import mihon.entry.interactions.book.processor.BookReaderProcessorRegistry
import mihon.entry.interactions.book.processor.BookReaderRequest
import mihon.entry.interactions.book.state.BookProgressLocatorCodec
import mihon.entry.viewer.settings.shared.ReaderCapabilityId
import okhttp3.OkHttpClient
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.model.EntryProgressState
import java.io.IOException
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
        url = "/chapter.html",
        name = "Chapter",
    )

    protected fun bookProgress(
        locator: BookLocator,
        completed: Boolean,
    ) = EntryProgressState(
        entryId = entry().id,
        chapterId = chapter().id,
        contentKey = "volume-1",
        resourceKey = "chapter.html",
        locator = BookProgressLocatorCodec.encode(locator),
        completed = completed,
    )

    protected suspend fun openWithProgress(
        chapter: EntryChapter,
        progress: EntryProgressState,
        preparedPublication: PreparedBookPublication = TestPublicationSession(
            readingOrder = listOf(
                BookResource(
                    id = "chapter-1.xhtml",
                    mediaType = "application/xhtml+xml",
                    title = null,
                ),
            ),
        ),
    ): OpenedBookReaderSession {
        val entry = entry()
        val source = mockk<UnifiedSource> {
            every { id } returns entry.source
            coEvery { getMedia(any(), any()) } returns EntryMedia.Book(
                descriptor = BookContentDescriptor("text/html"),
                publicationKeyOverride = "volume-1",
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
        val preparer = SessionFactoryTestPreparer(preparedPublication)
        val reader = SessionFactoryTestReaderProcessor(preparedPublication.model.descriptor)
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
                coEvery { get(entry.id, "volume-1", "chapter.html") } returns progress
                coEvery { getByEntryId(entry.id) } returns emptyList()
            },
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

        return assertIs<BookReaderOpenResult.Success>(
            factory.open(context, BookReaderRequest(entry.id, chapter.id), reader.id),
        ).session
    }

    protected fun emptyDownloadCache(): BookDownloadCache {
        val cache = mockk<BookDownloadCache>()
        coEvery { cache.getVerified(any()) } returns null
        return cache
    }

    protected fun failingDownloadCache(): BookDownloadCache = mockk {
        coEvery { getVerified(any()) } throws IOException("storage unavailable")
    }
}
internal class SessionFactoryTestPreparer(
    private val preparedPublication: PreparedBookPublication,
) : BookContentPreparer {
    override val id = "test.prose-preparer"
    override val outputModel = preparedPublication.model.descriptor
    var contentSession: BookContentSession? = null

    override fun supports(descriptor: BookContentDescriptor): Boolean =
        descriptor.format == "text/html"

    override suspend fun prepare(content: BookContentSession): BookPreparationResult {
        contentSession = content
        return BookPreparationResult.Success(preparedPublication)
    }
}

internal class SessionFactoryTestReaderProcessor(
    private val supportedModel: BookPublicationModelDescriptor = TEST_BOOK_MODEL_DESCRIPTOR,
) : BookReaderProcessor {
    override val id = "test.prose-reader"
    override val displayName = "Test prose"
    var receivedModel: BookPublicationModel? = null

    override fun supports(model: BookPublicationModelDescriptor): Boolean = model == supportedModel

    override fun createReaderIntent(
        context: Context,
        request: BookReaderRequest,
        sessionToken: String,
    ): Intent = Intent()

    override fun readerCapabilities(model: BookPublicationModel): Set<ReaderCapabilityId> {
        receivedModel = model
        return emptySet()
    }
}

internal class TestPublicationSession(
    readingOrder: List<BookResource> = emptyList(),
) : PreparedBookPublication {
    override val model = TestBookPublicationModel
    override val resourceLoader = TestBookPublicationResourceLoader
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
) : PreparedBookPublication {
    override val model = TestBookPublicationModel
    override val resourceLoader = TestBookPublicationResourceLoader
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

internal class LocatorRestorationPublicationSession(
    private val validResourceId: String,
    private val reconcile: suspend (BookLocator) -> BookLocator?,
) : PreparedBookPublication {
    override val model = TestBookPublicationModel
    override val resourceLoader = TestBookPublicationResourceLoader
    override val publication = BookPublication(
        id = "locator-restoration-book",
        revision = "1",
        title = "Locator Restoration Book",
        languages = emptyList(),
        readingDirection = null,
        readingOrder = listOf(BookResource(validResourceId, "application/xhtml+xml", null)),
        navigation = emptyList(),
    )

    override fun validate(locator: BookLocator): Boolean = locator.resourceId == validResourceId

    override suspend fun reconcileMigratedLocator(locator: BookLocator): BookLocator? = reconcile(locator)

    override fun close() = Unit
}

internal val TEST_BOOK_MODEL_DESCRIPTOR = BookPublicationModelDescriptor("test.book")

internal object TestBookPublicationModel : BookPublicationModel {
    override val descriptor = TEST_BOOK_MODEL_DESCRIPTOR
}

internal object TestBookPublicationResourceLoader : BookPublicationResourceLoader {
    override suspend fun load(
        resourceId: String,
        acceptedMediaTypes: Set<String>,
        maxBytes: Int,
    ): Result<BookPublicationResource> = Result.failure(
        IllegalArgumentException("No test BOOK resource $resourceId"),
    )
}
