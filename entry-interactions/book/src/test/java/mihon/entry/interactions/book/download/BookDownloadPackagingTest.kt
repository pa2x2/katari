package mihon.entry.interactions.book.download

import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.source.entry.BookResourceCatalog
import eu.kanade.tachiyomi.source.entry.BookResourceLocation
import eu.kanade.tachiyomi.source.entry.BookSourceResource
import eu.kanade.tachiyomi.source.entry.EntryCatalogueSource
import eu.kanade.tachiyomi.source.entry.EntryHttpSource
import eu.kanade.tachiyomi.source.entry.EntryMedia
import eu.kanade.tachiyomi.source.entry.EntryType
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import mihon.book.api.BookContentDescriptor
import mihon.entry.interactions.book.content.BookMaterializationCache
import mihon.entry.interactions.book.download.model.BookDownload
import mihon.entry.interactions.book.preparation.BookContentPreparerRegistry
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.source.service.SourceManager
import java.nio.file.Files
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
internal class BookDownloadPackagingTest : BookDownloaderFixture() {
    @Test
    fun `download publishes the primary prose chapter and all referenced resources`() = runTest {
        val application = RuntimeEnvironment.getApplication()
        val root = Files.createTempDirectory("book-download-root").toFile()
        val provider = BookDownloadProvider(downloadsDirectory = { UniFile.fromFile(root) })
        val cache = BookDownloadCache(provider)
        val materializationCache = BookMaterializationCache(
            application,
            Files.createTempDirectory("book-materialization").toFile(),
        )
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
            publicationRevision = "revision-1",
            catalog = BookResourceCatalog(
                resources = listOf(
                    BookSourceResource(
                        id = "chapter",
                        title = "Chapter",
                        mediaType = "text/html",
                        revision = "chapter-1",
                        location = BookResourceLocation.InlineText("<p>Offline</p>"),
                    ),
                    BookSourceResource(
                        id = "figure",
                        title = "Figure",
                        mediaType = "image/png",
                        revision = "figure-1",
                        location = BookResourceLocation.InlineBytes(byteArrayOf(1, 2, 3)),
                    ),
                ),
            ),
            initialResourceId = "chapter",
        )
        val source = mockk<EntryCatalogueSource> {
            every { id } returns entry.source
            every { name } returns "Fixture"
            every { lang } returns "en"
            coEvery { getMedia(any(), any()) } returns media
        }
        val sourceManager = mockk<SourceManager> {
            every { get(entry.source) } returns source
        }
        val networkHelper = mockk<NetworkHelper> {
            every { client } returns OkHttpClient()
        }
        val downloader = BookDownloader(
            application = application,
            provider = provider,
            cache = cache,
            sourceManager = sourceManager,
            networkHelper = networkHelper,
            materializationStore = materializationCache,
            preparerRegistry = BookContentPreparerRegistry(
                listOf(ValidatingPreparer(descriptor, requiredResourceIds = setOf("figure"))),
            ),
            now = { 123L },
        )
        val download = BookDownload(entry, chapter)

        val failure = downloader.download(download)

        assertNull(failure)
        assertEquals(BookDownload.State.DOWNLOADED, download.status)
        assertEquals(100, download.progress)
        val completed = cache.get(BookDownloadPackageKey(entry.source, entry.url, chapter.url))
        assertEquals(123L, completed?.manifest?.createdAt)
        assertEquals("chapter", completed?.manifest?.progressResourceId)
        assertEquals(listOf("en"), completed?.manifest?.languages)
        assertEquals(
            "<p>Offline</p>",
            completed?.resources?.get("chapter")?.openInputStream()?.reader()?.use { it.readText() },
        )
        assertEquals(
            listOf<Byte>(1, 2, 3),
            completed?.resources?.get("figure")?.openInputStream()?.use { it.readBytes().toList() },
        )
        assertEquals(setOf("chapter", "figure"), completed?.manifest?.resources?.map { it.id }?.toSet())
        assertTrue(provider.scanPackages().invalidPackageCount == 0)
    }

    @Test
    fun `download resolves remote resources with the source HTTP client`() = runTest {
        val application = RuntimeEnvironment.getApplication()
        val root = Files.createTempDirectory("book-download-root").toFile()
        val provider = BookDownloadProvider(downloadsDirectory = { UniFile.fromFile(root) })
        val cache = BookDownloadCache(provider)
        val materializationCache = BookMaterializationCache(
            application,
            Files.createTempDirectory("book-materialization").toFile(),
        )
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
            publicationRevision = "revision-1",
            catalog = BookResourceCatalog(
                resources = listOf(
                    BookSourceResource(
                        id = "chapter",
                        title = "Chapter",
                        mediaType = "text/html",
                        revision = "chapter-1",
                        location = BookResourceLocation.RemoteRequest("https://example.invalid/book"),
                    ),
                ),
            ),
            initialResourceId = "chapter",
        )
        val sourceRequests = AtomicInteger()
        val sourceClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = sourceRequests.incrementAndGet()
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(
                        (
                            if (request == 1) {
                                "<p>Authenticated A</p>"
                            } else {
                                "<p>Authenticated B</p>"
                            }
                            ).toResponseBody(),
                    )
                    .build()
            }
            .build()
        val globalRequests = AtomicInteger()
        val globalClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                globalRequests.incrementAndGet()
                error("Global HTTP client must not resolve source-owned BOOK resources: ${chain.request().url}")
            }
            .build()
        val source = mockk<EntryHttpSource> {
            every { id } returns entry.source
            every { name } returns "Fixture"
            every { lang } returns "en"
            every { client } returns sourceClient
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
                every { client } returns globalClient
            },
            materializationStore = materializationCache,
            preparerRegistry = BookContentPreparerRegistry(
                listOf(ValidatingPreparer(descriptor, "<p>Authenticated A</p>")),
            ),
        )

        val failure = downloader.download(BookDownload(entry, chapter))

        assertNull(failure)
        assertEquals(1, sourceRequests.get())
        assertEquals(0, globalRequests.get())
        assertEquals(
            "<p>Authenticated A</p>",
            cache.get(BookDownloadPackageKey(entry.source, entry.url, chapter.url))
                ?.resources
                ?.get("chapter")
                ?.openInputStream()
                ?.reader()
                ?.use { it.readText() },
        )
    }
}
