package mihon.translation.provider.libretranslate.server

import io.kotest.matchers.shouldBe
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.jupiter.api.Test

class LibreTranslateServerConfigurationTest {

    @Test
    fun `remote endpoints require HTTPS while loopback may use HTTP`() {
        LibreTranslateServerConfiguration.validateEndpoint("https://translate.example/api")
            ?.toString() shouldBe "https://translate.example/api/"
        LibreTranslateServerConfiguration.validateEndpoint("http://127.0.0.1:5000")
            ?.toString() shouldBe "http://127.0.0.1:5000/"
        LibreTranslateServerConfiguration.validateEndpoint("http://localhost:5000")
            ?.toString() shouldBe "http://localhost:5000/"
        LibreTranslateServerConfiguration.validateEndpoint("http://translate.example") shouldBe null
    }

    @Test
    fun `endpoint credentials query and fragment are rejected`() {
        LibreTranslateServerConfiguration.validateEndpoint("https://user:pass@example.com") shouldBe null
        LibreTranslateServerConfiguration.validateEndpoint("https://example.com?key=secret") shouldBe null
        LibreTranslateServerConfiguration.validateEndpoint("https://example.com/#fragment") shouldBe null
    }

    @Test
    fun `verified endpoint and API key persist through their separate stores`() {
        val state = FakeState()
        val secrets = FakeApiKeyStore()
        val configuration = LibreTranslateServerConfiguration(state, secrets)
        val endpoint = "https://translate.example/api/".toHttpUrl()

        configuration.save(endpoint, "private-key", verified = true)

        configuration.endpoint shouldBe endpoint
        configuration.apiKey shouldBe "private-key"
        configuration.isInitiallyVerified shouldBe true
        state.endpoint shouldBe endpoint.toString()
        state.verifiedEndpoint shouldBe endpoint.toString()
    }

    @Test
    fun `failed retest clears prior verification without discarding configuration`() {
        val endpoint = "https://translate.example/".toHttpUrl()
        val state = FakeState(
            endpoint = endpoint.toString(),
            verifiedEndpoint = endpoint.toString(),
        )
        val secrets = FakeApiKeyStore("old-key")
        val configuration = LibreTranslateServerConfiguration(state, secrets)

        configuration.save(endpoint, "new-key", verified = false)

        configuration.endpoint shouldBe endpoint
        configuration.apiKey shouldBe "new-key"
        configuration.isInitiallyVerified shouldBe false
        state.verifiedEndpoint shouldBe null
    }

    private class FakeState(
        override var endpoint: String? = null,
        override var verifiedEndpoint: String? = null,
        override var disclosureAccepted: Boolean = false,
    ) : LibreTranslateServerStateStore {
        override fun saveEndpoint(
            endpoint: String,
            verifiedEndpoint: String?,
        ) {
            this.endpoint = endpoint
            this.verifiedEndpoint = verifiedEndpoint
        }
    }

    private class FakeApiKeyStore(
        private var value: String? = null,
    ) : LibreTranslateApiKeyStore {
        override fun read() = value

        override fun write(value: String) {
            this.value = value
        }
    }
}
