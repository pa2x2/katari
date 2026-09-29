package mihon.entry.interactions.book.content

import android.content.ContentResolver
import android.content.Context
import eu.kanade.tachiyomi.source.entry.BookResourceLocation
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@RunWith(RobolectricTestRunner::class)
class AndroidBookExternalResourceResolverTest {

    @Test
    fun `remote requests keep source headers and emulate ranges when server returns full content`() = runBlocking {
        val capturedRequest = AtomicReference<Request>()
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                capturedRequest.set(chain.request())
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body("0123456789".toResponseBody())
                    .build()
            }
            .build()
        val resolver = AndroidBookExternalResourceResolver(context(), client)

        resolver.open(
            BookResourceLocation.RemoteRequest(
                url = "https://example.invalid/book",
                headers = mapOf("Authorization" to "secret"),
            ),
            BookByteRange(2, 5),
        ).use { opened ->
            assertEquals("234", opened.stream.bufferedReader().readText())
        }

        assertEquals("secret", capturedRequest.get().header("Authorization"))
        assertEquals("bytes=2-4", capturedRequest.get().header("Range"))
    }

    @Test
    fun `partial remote response must identify the requested start offset`() = runBlocking {
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(206)
                    .message("Partial Content")
                    .header("Content-Range", "bytes 4-6/10")
                    .body("456".toResponseBody())
                    .build()
            }
            .build()
        val resolver = AndroidBookExternalResourceResolver(context(), client)

        assertFailsWith<IOException> {
            resolver.open(
                BookResourceLocation.RemoteRequest("https://example.invalid/book"),
                BookByteRange(2, 5),
            )
        }
        Unit
    }

    @Test
    fun `https redirect downgrade is rejected before extension headers can be sent over HTTP`() = runBlocking {
        val requestCount = AtomicInteger()
        val capturedRequest = AtomicReference<Request>()
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                requestCount.incrementAndGet()
                capturedRequest.set(chain.request())
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(302)
                    .message("Found")
                    .header("Location", "http://example.invalid/book")
                    .body("redirect".toResponseBody())
                    .build()
            }
            .build()
        val resolver = AndroidBookExternalResourceResolver(context(), client)

        assertFailsWith<IOException> {
            resolver.open(
                BookResourceLocation.RemoteRequest(
                    "https://example.invalid/book",
                    headers = mapOf("X-Extension-Token" to "secret"),
                ),
                null,
            )
        }
        assertEquals(1, requestCount.get())
        assertEquals("https", capturedRequest.get().url.scheme)
        assertEquals("secret", capturedRequest.get().header("X-Extension-Token"))
        Unit
    }

    @Test
    fun `cross-origin https redirects retain only explicitly safe request headers`() = runBlocking {
        val requestCount = AtomicInteger()
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val count = requestCount.incrementAndGet()
                if (count == 1) {
                    assertEquals("secret", chain.request().header("Authorization"))
                    Response.Builder()
                        .request(chain.request())
                        .protocol(Protocol.HTTP_1_1)
                        .code(302)
                        .message("Found")
                        .header("Location", "https://cdn.example.invalid/book")
                        .body("redirect".toResponseBody())
                        .build()
                } else {
                    assertEquals("https://cdn.example.invalid/book", chain.request().url.toString())
                    assertEquals(null, chain.request().header("Authorization"))
                    assertEquals(null, chain.request().header("Cookie"))
                    assertEquals(null, chain.request().header("Proxy-Authorization"))
                    assertEquals(null, chain.request().header("X-Api-Key"))
                    assertEquals(null, chain.request().header("X-Extension-Token"))
                    assertEquals("application/xhtml+xml", chain.request().header("Accept"))
                    assertEquals("bytes=2-4", chain.request().header("Range"))
                    Response.Builder()
                        .request(chain.request())
                        .protocol(Protocol.HTTP_1_1)
                        .code(200)
                        .message("OK")
                        .body("secure".toResponseBody())
                        .build()
                }
            }
            .build()
        val resolver = AndroidBookExternalResourceResolver(context(), client)

        resolver.open(
            BookResourceLocation.RemoteRequest(
                "https://example.invalid/book",
                headers = mapOf(
                    "Authorization" to "secret",
                    "Cookie" to "session=secret",
                    "Proxy-Authorization" to "proxy-secret",
                    "X-Api-Key" to "api-secret",
                    "X-Extension-Token" to "source-value",
                    "Accept" to "application/xhtml+xml",
                    "Range" to "bytes=2-4",
                ),
            ),
            null,
        ).use { opened ->
            assertEquals("secure", opened.stream.bufferedReader().readText())
        }

        assertEquals(2, requestCount.get())
    }

    private fun context(): Context {
        val context = mockk<Context>()
        every { context.applicationContext } returns context
        every { context.contentResolver } returns mockk<ContentResolver>(relaxed = true)
        return context
    }
}
