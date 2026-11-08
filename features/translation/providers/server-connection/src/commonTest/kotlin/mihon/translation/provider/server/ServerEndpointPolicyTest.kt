package mihon.translation.provider.server

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class ServerEndpointPolicyTest {

    @Test
    fun `remote endpoints require HTTPS and no endpoint may carry credentials, a query, or a fragment`() {
        ServerEndpointPolicy.validate("https://translate.example/api")
            ?.toString() shouldBe "https://translate.example/api/"
        ServerEndpointPolicy.validate("http://127.0.0.1:5000")
            ?.toString() shouldBe "http://127.0.0.1:5000/"
        ServerEndpointPolicy.validate("http://localhost:5000")
            ?.toString() shouldBe "http://localhost:5000/"
        ServerEndpointPolicy.validate("http://translate.example") shouldBe null
        ServerEndpointPolicy.validate("https://user:pass@example.com") shouldBe null
        ServerEndpointPolicy.validate("https://example.com?key=secret") shouldBe null
        ServerEndpointPolicy.validate("https://example.com/#fragment") shouldBe null
    }
}
