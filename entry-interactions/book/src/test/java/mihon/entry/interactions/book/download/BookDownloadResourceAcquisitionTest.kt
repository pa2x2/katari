package mihon.entry.interactions.book.download

import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.source.entry.BookResourceCatalog
import eu.kanade.tachiyomi.source.entry.BookResourceLocation
import eu.kanade.tachiyomi.source.entry.BookSourceResource
import eu.kanade.tachiyomi.source.entry.EntryMedia
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import mihon.book.api.BookContentDescriptor
import mihon.entry.interactions.book.BookContentPreparerRegistry
import mihon.entry.interactions.book.BookMaterializationCache
import mihon.entry.interactions.book.document.resource.PROSE_IMAGE_RESOURCE_REQUIREMENT
import mihon.entry.interactions.book.download.model.BookDownload
import mihon.entry.interactions.book.download.model.BookDownloadFailure
import okhttp3.OkHttpClient
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import java.nio.file.Files
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
internal class BookDownloadResourceAcquisitionTest : BookDownloaderFixture() {
    @Test
    fun `download fails without publishing when a required prose resource is unavailable`() = runTest {
        val application = RuntimeEnvironment.getApplication()
        val root = Files.createTempDirectory("book-download-root").toFile()
        val provider = BookDownloadProvider(downloadsDirectory = { UniFile.fromFile(root) })
        val cache = BookDownloadCache(provider)
        val entry = Entry.create().copy(
            id = 1L,
            profileId = 2L,
            source = 42L,
            url = "/books/test",
            title = "Test Book",
            type = EntryType.BOOK,
        )
        val chapter = EntryChapter.create().copy(
            id = 10L,
            entryId = entry.id,
            url = "/books/test/chapter",
            name = "Chapter",
        )
        val descriptor = BookContentDescriptor("text/html", profile = "prose")
        val source = mockk<UnifiedSource> {
            every { id } returns entry.source
            every { name } returns "Fixture"
            coEvery { getMedia(any(), any()) } returns EntryMedia.Book(
                descriptor = descriptor,
                catalog = BookResourceCatalog(
                    resources = listOf(
                        BookSourceResource(
                            id = "chapter",
                            title = "Chapter",
                            mediaType = "text/html",
                            location = BookResourceLocation.InlineText("<p>Offline</p>"),
                        ),
                    ),
                ),
                initialResourceId = "chapter",
            )
        }
        val downloader = BookDownloader(
            application = application,
            provider = provider,
            cache = cache,
            sourceManager = mockk {
                every { get(entry.source) } returns source
            },
            networkHelper = mockk {
                every { client } returns OkHttpClient()
            },
            materializationStore = BookMaterializationCache(
                application,
                Files.createTempDirectory("book-materialization").toFile(),
            ),
            preparerRegistry = BookContentPreparerRegistry(
                listOf(ValidatingPreparer(descriptor, requiredResourceIds = setOf("missing-figure"))),
            ),
        )

        val failure = downloader.download(BookDownload(entry, chapter))

        assertNotNull(failure)
        assertEquals(BookDownloadFailure.Reason.NETWORK, failure.reason)
        assertNull(cache.get(BookDownloadPackageKey(entry.source, entry.url, chapter.url)))
        assertTrue(provider.scanPackages().packages.isEmpty())
    }

    @Test
    fun `download rejects a required resource that exceeds its renderer byte constraint`() = runTest {
        val application = RuntimeEnvironment.getApplication()
        val root = Files.createTempDirectory("book-download-root").toFile()
        val provider = BookDownloadProvider(downloadsDirectory = { UniFile.fromFile(root) })
        val cache = BookDownloadCache(provider)
        val entry = Entry.create().copy(
            id = 1L,
            profileId = 2L,
            source = 42L,
            url = "/books/test",
            title = "Test Book",
            type = EntryType.BOOK,
        )
        val chapter = EntryChapter.create().copy(
            id = 10L,
            entryId = entry.id,
            url = "/books/test/chapter",
            name = "Chapter",
        )
        val descriptor = BookContentDescriptor("text/html", profile = "prose")
        val media = EntryMedia.Book(
            descriptor = descriptor,
            catalog = BookResourceCatalog(
                resources = listOf(
                    BookSourceResource(
                        id = "chapter",
                        title = "Chapter",
                        mediaType = "text/html",
                        location = BookResourceLocation.InlineText("<p>Offline</p>"),
                    ),
                    BookSourceResource(
                        id = "figure",
                        title = "Oversized figure",
                        mediaType = "image/png",
                        location = BookResourceLocation.InlineBytes(byteArrayOf(1, 2, 3)),
                    ),
                ),
            ),
            initialResourceId = "chapter",
        )
        val source = mockk<UnifiedSource> {
            every { id } returns entry.source
            every { name } returns "Fixture"
            coEvery { getMedia(any(), any()) } returns media
        }
        val downloader = BookDownloader(
            application = application,
            provider = provider,
            cache = cache,
            sourceManager = mockk {
                every { get(entry.source) } returns source
            },
            networkHelper = mockk {
                every { client } returns OkHttpClient()
            },
            materializationStore = BookMaterializationCache(
                application,
                Files.createTempDirectory("book-materialization").toFile(),
            ),
            preparerRegistry = BookContentPreparerRegistry(
                listOf(
                    ValidatingPreparer(
                        descriptor = descriptor,
                        requiredResourceIds = setOf("figure"),
                        resourceRequirements = mapOf(
                            "figure" to PROSE_IMAGE_RESOURCE_REQUIREMENT.copy(maxBytes = 2),
                        ),
                    ),
                ),
            ),
        )

        val failure = downloader.download(BookDownload(entry, chapter))

        assertNotNull(failure)
        assertEquals(BookDownloadFailure.Reason.INTEGRITY, failure.reason)
        assertNull(cache.get(BookDownloadPackageKey(entry.source, entry.url, chapter.url)))
        assertTrue(provider.scanPackages().packages.isEmpty())
    }
}
