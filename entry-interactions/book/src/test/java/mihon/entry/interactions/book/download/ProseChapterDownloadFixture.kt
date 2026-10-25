package mihon.entry.interactions.book.download

import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.source.entry.BookLinkedResources
import eu.kanade.tachiyomi.source.entry.BookResourceCatalog
import eu.kanade.tachiyomi.source.entry.BookResourceLocation
import eu.kanade.tachiyomi.source.entry.BookSourceResource
import eu.kanade.tachiyomi.source.entry.EntryMedia
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import mihon.book.api.BookContentDescriptor
import mihon.entry.interactions.book.content.BookMaterializationCache
import mihon.entry.interactions.book.download.model.BookDownload
import mihon.entry.interactions.book.download.model.BookDownloadFailure
import mihon.entry.interactions.book.format.html.prosechapter.preparation.HtmlProseChapterPreparer
import mihon.entry.interactions.book.preparation.BookContentPreparerRegistry
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import javax.imageio.ImageIO

/** Downloads one prose chapter through the real prose preparer, with image hosts answered by [network]. */
@RunWith(RobolectricTestRunner::class)
internal abstract class ProseChapterDownloadFixture {
    protected val pngBytes: ByteArray = ByteArrayOutputStream().also { output ->
        ImageIO.write(BufferedImage(4, 3, BufferedImage.TYPE_INT_RGB), "png", output)
    }.toByteArray()

    protected val materializationStore by lazy {
        BookMaterializationCache(
            RuntimeEnvironment.getApplication(),
            Files.createTempDirectory("prose-download-materialization").toFile(),
        )
    }

    protected suspend fun TestScope.downloadProseChapter(
        html: String,
        network: Interceptor,
        linkedResources: BookLinkedResources = BookLinkedResources(),
        catalogImages: Map<String, ByteArray> = emptyMap(),
    ): ProseChapterDownloadResult {
        val application = RuntimeEnvironment.getApplication()
        val downloadRoot = Files.createTempDirectory("prose-download").toFile()
        val provider = BookDownloadProvider(downloadsDirectory = { UniFile.fromFile(downloadRoot) })
        val cache = BookDownloadCache(provider, backgroundScope)
        val entry = Entry.create().copy(
            id = 201L,
            source = 242L,
            url = "/novels/fixture",
            title = "Fixture Novel",
            type = EntryType.BOOK,
        )
        val chapter = EntryChapter.create().copy(
            id = 210L,
            entryId = entry.id,
            url = "/novels/fixture/1",
            name = "Chapter 1",
        )
        val location = BookResourceLocation.InlineText(html, "text/html")
        val media = EntryMedia.Book(
            descriptor = BookContentDescriptor("text/html", profile = "prose-chapter"),
            catalog = BookResourceCatalog(
                resources = listOf(BookSourceResource(id = "chapter", mediaType = "text/html", location = location)) +
                    catalogImages.map { (id, bytes) ->
                        BookSourceResource(id = id, location = BookResourceLocation.InlineBytes(bytes, "image/png"))
                    },
            ),
            initialResourceId = "chapter",
            initialResourceLocation = location,
            linkedResources = linkedResources,
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
                every { isInitialized } returns MutableStateFlow(true)
                every { get(entry.source) } returns source
            },
            networkHelper = mockk {
                every { client } returns OkHttpClient.Builder().addInterceptor(network).build()
            },
            materializationStore = materializationStore,
            preparerRegistry = BookContentPreparerRegistry(listOf(HtmlProseChapterPreparer())),
        )

        val failure = downloader.download(BookDownload(entry, chapter))
        return ProseChapterDownloadResult(
            failure = failure,
            completedPackage = cache.get(BookDownloadPackageKey(entry.source, entry.url, chapter.url)),
        )
    }

    protected fun Interceptor.Chain.respond(code: Int, body: ByteArray = ByteArray(0)): Response = Response.Builder()
        .request(request())
        .protocol(Protocol.HTTP_1_1)
        .code(code)
        .message("Fixture")
        .body(body.toResponseBody("application/octet-stream".toMediaType()))
        .build()
}

internal data class ProseChapterDownloadResult(
    val failure: BookDownloadFailure?,
    val completedPackage: VerifiedBookDownloadPackage?,
)
