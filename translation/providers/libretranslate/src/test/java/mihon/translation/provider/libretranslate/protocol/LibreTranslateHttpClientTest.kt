package mihon.translation.provider.libretranslate.protocol

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.jupiter.api.Test

class LibreTranslateHttpClientTest {

    @Test
    fun `translation uses the LibreTranslate JSON contract with the API key only in the body`() = runTest {
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
                method shouldBe "POST"
                url.encodedPath shouldBe "/translate"
                url.query shouldBe null
                body?.utf8() shouldBe
                    """{"q":"Hello","source":"en","target":"fr","api_key":"private-key"}"""
            }
        }
    }
}
