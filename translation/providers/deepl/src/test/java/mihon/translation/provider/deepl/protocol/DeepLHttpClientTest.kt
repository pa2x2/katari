package mihon.translation.provider.deepl.protocol

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.jupiter.api.Test

class DeepLHttpClientTest {

    @Test
    fun `texts are sent together with their context and the API key only in the authorization header`() = runTest {
        MockWebServer().use { server ->
            server.enqueue(
                MockResponse.Builder()
                    .body(
                        """
                        {"translations":[
                          {"detected_source_language":"JA","text":"Good morning.","model_type_used":"quality_optimized"},
                          {"detected_source_language":"JA","text":"You're late."}
                        ]}
                        """.trimIndent(),
                    )
                    .build(),
            )
            server.start()
            val client = DeepLHttpClient(
                httpClient = OkHttpClient(),
                endpoint = server.url("/"),
                apiKey = "private-key",
            )

            client.translate(
                texts = listOf("おはよう", "遅いよ"),
                source = "JA",
                target = "EN-US",
                context = "Title: Example",
            ) shouldBe listOf("Good morning.", "You're late.")

            server.takeRequest().apply {
                method shouldBe "POST"
                url.encodedPath shouldBe "/v2/translate"
                url.query shouldBe null
                headers["Authorization"] shouldBe "DeepL-Auth-Key private-key"
                body?.utf8() shouldBe
                    """{"text":["おはよう","遅いよ"],"source_lang":"JA","target_lang":"EN-US","context":"Title: Example"}"""
            }
        }
    }
}
