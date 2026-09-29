package mihon.translation.provider.libretranslate.server

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class LibreTranslateServerConfigurationTest {

    @Test
    fun `remote endpoints require HTTPS and no endpoint may carry credentials, a query, or a fragment`() {
        LibreTranslateServerConfiguration.validateEndpoint("https://translate.example/api")
            ?.toString() shouldBe "https://translate.example/api/"
        LibreTranslateServerConfiguration.validateEndpoint("http://127.0.0.1:5000")
            ?.toString() shouldBe "http://127.0.0.1:5000/"
        LibreTranslateServerConfiguration.validateEndpoint("http://localhost:5000")
            ?.toString() shouldBe "http://localhost:5000/"
        LibreTranslateServerConfiguration.validateEndpoint("http://translate.example") shouldBe null
        LibreTranslateServerConfiguration.validateEndpoint("https://user:pass@example.com") shouldBe null
        LibreTranslateServerConfiguration.validateEndpoint("https://example.com?key=secret") shouldBe null
        LibreTranslateServerConfiguration.validateEndpoint("https://example.com/#fragment") shouldBe null
    }
}
