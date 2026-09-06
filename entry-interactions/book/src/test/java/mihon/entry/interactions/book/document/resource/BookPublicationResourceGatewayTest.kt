package mihon.entry.interactions.book.document.resource

import kotlinx.coroutines.test.runTest
import mihon.entry.interactions.book.preparation.BookPublicationResource
import mihon.entry.interactions.book.preparation.BookPublicationResourceLoader
import mihon.entry.interactions.book.preparation.BookRemoteResourceReference
import mihon.entry.interactions.book.preparation.BookRemoteResourceType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BookPublicationResourceGatewayTest {
    @Test
    fun `remote resource cannot request before origin approval and approved bytes are snapshotted`() = runTest {
        val requests = AtomicInteger()
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            requests.incrementAndGet()
            Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(byteArrayOf(1, 2, 3).toResponseBody("image/png".toMediaType()))
                .build()
        }.build()
        val directory = Files.createTempDirectory("book-remote-resource").toFile()
        val reference = BookRemoteResourceReference(
            resourceId = "remote-image",
            url = "https://assets.example/image.png",
            type = BookRemoteResourceType.IMAGE,
        )
        val gateway = gateway(client, directory, reference)

        assertTrue(gateway.load("remote-image", setOf("image/png"), 1024).isFailure)
        assertEquals(0, requests.get())

        gateway.authorizeRemoteOrigins(setOf("https://assets.example:443"))
        assertEquals(
            byteArrayOf(1, 2, 3).toList(),
            gateway.load("remote-image", setOf("image/png"), 1024)
                .getOrThrow().bytes.toList(),
        )
        assertEquals(1, requests.get())

        val restored = gateway(client, directory, reference)
        assertTrue(restored.load("remote-image", setOf("image/png"), 1024).isFailure)
        restored.authorizeRemoteOrigins(setOf("https://assets.example:443"))
        assertTrue(restored.load("remote-image", setOf("image/png"), 1024).isSuccess)
        assertEquals(1, requests.get())
        directory.deleteRecursively()
    }

    @Test
    fun `redirect to an unapproved origin is stopped before the redirected request`() = runTest {
        val requests = AtomicInteger()
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            requests.incrementAndGet()
            Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(302)
                .message("Found")
                .header("Location", "https://other.example/image.png")
                .body(byteArrayOf().toResponseBody())
                .build()
        }.build()
        val directory = Files.createTempDirectory("book-remote-redirect").toFile()
        val gateway = gateway(
            client,
            directory,
            BookRemoteResourceReference(
                "remote-image",
                "https://assets.example/image.png",
                BookRemoteResourceType.IMAGE,
            ),
        )
        gateway.authorizeRemoteOrigins(setOf("https://assets.example:443"))

        assertTrue(gateway.load("remote-image", setOf("image/png"), 1024).isFailure)
        assertEquals(1, requests.get())
        directory.deleteRecursively()
    }

    private fun gateway(
        client: OkHttpClient,
        directory: java.io.File,
        reference: BookRemoteResourceReference,
    ) = BookPublicationResourceGateway(
        packaged = object : BookPublicationResourceLoader {
            override suspend fun load(
                resourceId: String,
                acceptedMediaTypes: Set<String>,
                maxBytes: Int,
            ): Result<BookPublicationResource> = error("Unexpected packaged resource")
        },
        references = listOf(reference),
        httpClient = client,
        snapshotDirectory = directory,
    )
}
