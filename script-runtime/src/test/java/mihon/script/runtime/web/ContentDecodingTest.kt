package mihon.script.runtime.web

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import mihon.script.runtime.ScriptNetwork
import mihon.script.runtime.ScriptRuntimeFactory
import mihon.script.runtime.ScriptRuntimeSpec
import mihon.script.runtime.ScriptSource
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import okio.Buffer
import okio.GzipSink
import okio.buffer
import org.junit.jupiter.api.Test

class ContentDecodingTest {

    // Tensurafan asks GitHub Pages for the first bytes of a page with Range, and gets them gzip-encoded: the stream is
    // cut off at the range's end, and decoding it as a whole body failed the request
    @Test
    fun `a range of a gzip-encoded page gives what decodes from it`() = runBlocking<Unit> {
        val page = "<img src=\"cover.jpg\">" + "text ".repeat(10_000)
        val gzipped = Buffer().also { buffer -> GzipSink(buffer).buffer().use { it.writeUtf8(page) } }.readByteArray()
        MockWebServer().use { server ->
            server.enqueue(
                MockResponse.Builder()
                    .code(206)
                    .addHeader("Content-Encoding", "gzip")
                    .body(Buffer().write(gzipped, 0, gzipped.size / 2))
                    .build(),
            )
            server.start()
            val runtime = ScriptRuntimeFactory(ScriptNetwork(OkHttpClient()) { "test" }).open(
                ScriptRuntimeSpec(
                    name = "range",
                    scripts = listOf(
                        ScriptSource(
                            "range.js",
                            """
                            async function head(url) {
                              var response = await fetch(url, { headers: { Range: 'bytes=0-99', 'Accept-Encoding': 'gzip' } });
                              return (await response.text()).slice(0, 21);
                            }
                            """.trimIndent(),
                        ),
                    ),
                ),
            )

            runtime.use {
                it.call("head", JsonArray(listOf(JsonPrimitive(server.url("/").toString()))))
            } shouldBe JsonPrimitive("<img src=\"cover.jpg\">")
        }
    }
}
