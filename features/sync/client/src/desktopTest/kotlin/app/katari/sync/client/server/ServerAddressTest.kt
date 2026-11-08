package app.katari.sync.client.server

import io.kotest.matchers.shouldBe
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.jupiter.api.Test

class ServerAddressTest {

    @Test
    fun `only a local-network address typed without a scheme falls back to plain http`() {
        ServerAddress.parse("nas.local") shouldBe accepted("https://nas.local/", fallback = "http://nas.local/")
        ServerAddress.parse("192.168.1.5:8080") shouldBe
            accepted("https://192.168.1.5:8080/", fallback = "http://192.168.1.5:8080/")
        ServerAddress.parse("homeserver") shouldBe accepted("https://homeserver/", fallback = "http://homeserver/")

        ServerAddress.parse("sync.example.com") shouldBe accepted("https://sync.example.com/", fallback = null)
        ServerAddress.parse("https://nas.local") shouldBe accepted("https://nas.local/", fallback = null)
        ServerAddress.parse("http://nas.local:8080") shouldBe accepted("http://nas.local:8080/", fallback = null)
        ServerAddress.parse("http://sync.example.com") shouldBe ServerAddress.PlainHttpOutsideLocalNetwork
    }

    private fun accepted(url: String, fallback: String?) =
        ServerAddress.Accepted(url.toHttpUrl(), fallback?.toHttpUrl())
}
