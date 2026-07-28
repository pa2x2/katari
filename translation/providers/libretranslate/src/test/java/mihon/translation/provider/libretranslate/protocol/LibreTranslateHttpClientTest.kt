package mihon.translation.provider.libretranslate.protocol

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit

class LibreTranslateHttpClientTest {

    @Test
    fun `language capabilities come from the provider catalog`() = runTest {
        MockWebServer().use { server ->
            server.enqueue(
                MockResponse.Builder()
                    .body(
                        """
                        [
                          {"code":"en","name":"English","targets":["fr","pl"]},
                          {"code":"fr","name":"French","targets":["en"]}
                        ]
                        """.trimIndent(),
                    )
                    .build(),
            )

            val languages = client(server).languages()

            languages shouldContainExactly listOf(
                LibreTranslateLanguage("en", "English", setOf("fr", "pl")),
                LibreTranslateLanguage("fr", "French", setOf("en")),
            )
            server.takeRequest().apply {
                method shouldBe "GET"
                url.encodedPath shouldBe "/languages"
            }
        }
    }

    @Test
    fun `translation uses the LibreTranslate JSON contract`() = runTest {
        MockWebServer().use { server ->
            server.enqueue(
                MockResponse.Builder()
                    .body("""{"translatedText":"Bonjour"}""")
                    .build(),
            )

            client(server).translate("Hello", "en", "fr") shouldBe "Bonjour"

            server.takeRequest().apply {
                method shouldBe "POST"
                url.encodedPath shouldBe "/translate"
                body?.utf8() shouldBe
                    """{"q":"Hello","source":"en","target":"fr"}"""
            }
        }
    }

    @Test
    fun `optional API key is sent only in the translation body`() = runTest {
        MockWebServer().use { server ->
            server.enqueue(
                MockResponse.Builder()
                    .body("""{"translatedText":"Bonjour"}""")
                    .build(),
            )
            server.start()
            val client = LibreTranslateHttpClient(
                httpClient = OkHttpClient(),
                endpoint = server.url("/"),
                apiKey = "private-key",
            )

            client.translate("Hello", "en", "fr") shouldBe "Bonjour"

            server.takeRequest().apply {
                url.query shouldBe null
                body?.utf8() shouldBe
                    """{"q":"Hello","source":"en","target":"fr","api_key":"private-key"}"""
            }
        }
    }

    @Test
    fun `provider failures do not expose response payloads`() = runTest {
        MockWebServer().use { server ->
            server.enqueue(
                MockResponse.Builder()
                    .code(400)
                    .body("""{"error":"selected text must never escape diagnostics"}""")
                    .build(),
            )

            val error = shouldThrow<LibreTranslateException> {
                client(server).translate("private selection", "en", "fr")
            }

            error.kind shouldBe LibreTranslateFailureKind.Rejected
            error.message.orEmpty() shouldNotContain "private selection"
            error.message.orEmpty() shouldNotContain "selected text"
        }
    }

    @Test
    fun `cancelling a request cancels the HTTP wait`() = runTest {
        MockWebServer().use { server ->
            server.enqueue(
                MockResponse.Builder()
                    .body("""{"translatedText":"late"}""")
                    .bodyDelay(30, TimeUnit.SECONDS)
                    .build(),
            )
            val translation = async {
                client(server).translate("Hello", "en", "fr")
            }

            runCurrent()
            (server.takeRequest(5, TimeUnit.SECONDS) != null) shouldBe true
            translation.cancel()

            shouldThrow<CancellationException> { translation.await() }
        }
    }

    private fun client(server: MockWebServer): LibreTranslateHttpClient {
        server.start()
        return LibreTranslateHttpClient(
            httpClient = OkHttpClient(),
            endpoint = server.url("/"),
        )
    }
}
