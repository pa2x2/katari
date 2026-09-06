package mihon.entry.interactions.book.format.epub.preparation

import kotlinx.coroutines.test.runTest
import mihon.book.api.document.BookDocumentBlockContent
import mihon.entry.interactions.book.document.preparation.PreparedBookDocumentPublication
import mihon.entry.interactions.book.document.resource.BookPublicationResourceGatewayFactory
import mihon.entry.interactions.book.format.epub.archive.epubArchiveFile
import mihon.entry.interactions.book.preparation.BookPreparationResult
import mihon.entry.interactions.book.preparation.BookRemoteResourceRequest
import mihon.entry.interactions.book.preparation.BookRemoteResourceType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class EpubRemoteResourcesTest {
    @Test
    fun `declared remote image reaches the consent gateway without blocking local reading`() = runTest {
        val requests = AtomicInteger()
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            requests.incrementAndGet()
            assertEquals("https://assets.example/image.png", chain.request().url.toString())
            Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(byteArrayOf(1, 2, 3).toResponseBody("image/png".toMediaType()))
                .build()
        }.build()
        val gatewayFactory = BookPublicationResourceGatewayFactory(RuntimeEnvironment.getApplication(), client)
        val file = epubArchiveFile(
            mapOf(
                "META-INF/container.xml" to """
                    <container xmlns="urn:oasis:names:tc:opendocument:xmlns:container">
                      <rootfiles><rootfile full-path="package.opf"/></rootfiles>
                    </container>
                """.trimIndent(),
                "package.opf" to """
                    <package xmlns="http://www.idpf.org/2007/opf">
                      <manifest>
                        <item id="chapter" href="chapter.xhtml" media-type="application/xhtml+xml" properties="remote-resources"/>
                        <item id="image" href="https://assets.example/image.png" media-type="image/png"/>
                        <item id="video" href="https://assets.example/video.mp4" media-type="video/mp4"/>
                      </manifest>
                      <spine><itemref idref="chapter"/></spine>
                    </package>
                """.trimIndent(),
                "chapter.xhtml" to """
                    <html xmlns="http://www.w3.org/1999/xhtml"><body>
                      <p>Local reading remains available.</p>
                      <img src="https://assets.example/image.png" alt="Illustration"/>
                    </body></html>
                """.trimIndent(),
            ),
        )
        try {
            val result = EpubBookPreparer(
                resourceGatewayFactory = gatewayFactory,
            ).prepare(EpubContentSessionFixture(file))
            val prepared = assertIs<BookPreparationResult.Success>(result).publication
            prepared.use {
                val publication = assertIs<PreparedBookDocumentPublication>(prepared)
                assertTrue(publication.documents.single().blocks.any { it.plainText.contains("Local reading") })
                assertEquals(
                    setOf(BookRemoteResourceRequest("https://assets.example:443", BookRemoteResourceType.IMAGE)),
                    publication.remoteResourceRequests,
                )
                val image = publication.documents.single().blocks.mapNotNull {
                    (it.content as? BookDocumentBlockContent.Figure)?.image
                }.single()
                assertTrue(publication.resourceLoader.load(image.resourceId, setOf("image/png"), 1024).isFailure)
                assertEquals(0, requests.get())
                publication.authorizeRemoteOrigins(setOf("https://assets.example:443"))
                assertEquals(
                    listOf<Byte>(1, 2, 3),
                    publication.resourceLoader.load(
                        image.resourceId,
                        setOf("image/png"),
                        1024,
                    ).getOrThrow().bytes.toList(),
                )
                assertEquals(1, requests.get())
            }
        } finally {
            gatewayFactory.removePublication("publication")
            file.delete()
        }
    }
}
