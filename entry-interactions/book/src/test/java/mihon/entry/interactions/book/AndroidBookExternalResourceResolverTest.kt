package mihon.entry.interactions.book

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
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
import java.io.ByteArrayInputStream
import java.io.InputStream
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
    fun `local content URI range is bounded inside Katari`() = runBlocking {
        val contentResolver = mockk<ContentResolver>()
        every { contentResolver.openInputStream(Uri.parse("content://app.katari/book/1")) } returns
            ByteArrayInputStream("local-content".encodeToByteArray())
        val resolver = AndroidBookExternalResourceResolver(
            context = context(contentResolver),
            httpClient = OkHttpClient(),
        )

        resolver.open(
            BookResourceLocation.LocalUri("content://app.katari/book/1"),
            BookByteRange(6, 13),
        ).use { opened ->
            assertEquals("content", opened.stream.bufferedReader().readText())
        }
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

        assertFailsWith<java.io.IOException> {
            resolver.open(
                BookResourceLocation.RemoteRequest("https://example.invalid/book"),
                BookByteRange(2, 5),
            )
        }
        Unit
    }

    @Test
    fun `app references remain behind an app-owned resolver`() = runBlocking {
        val appResolver = object : BookAppReferenceResolver {
            override suspend fun open(id: String, range: BookByteRange?): ExternalBookResource {
                assertEquals("download:42", id)
                assertEquals(BookByteRange(0, 3), range)
                return external("app")
            }
        }
        val resolver = AndroidBookExternalResourceResolver(
            context = context(),
            httpClient = OkHttpClient(),
            appReferenceResolver = appResolver,
        )

        resolver.open(BookResourceLocation.AppReference("download:42"), BookByteRange(0, 3)).use { opened ->
            assertEquals("app", opened.stream.bufferedReader().readText())
        }
    }

    @Test
    fun `unregistered app reference and internal locations fail explicitly`() = runBlocking {
        val resolver = AndroidBookExternalResourceResolver(context(), OkHttpClient())

        assertFailsWith<IllegalStateException> {
            resolver.open(BookResourceLocation.AppReference("missing"), null)
        }
        assertFailsWith<IllegalStateException> {
            resolver.open(BookResourceLocation.InlineText("internal"), null)
        }
        Unit
    }

    private fun context(contentResolver: ContentResolver = mockk(relaxed = true)): Context {
        val context = mockk<Context>()
        every { context.applicationContext } returns context
        every { context.contentResolver } returns contentResolver
        return context
    }

    private fun external(content: String): ExternalBookResource {
        return object : ExternalBookResource {
            override val stream: InputStream = ByteArrayInputStream(content.encodeToByteArray())
            override fun close() = stream.close()
        }
    }
}
