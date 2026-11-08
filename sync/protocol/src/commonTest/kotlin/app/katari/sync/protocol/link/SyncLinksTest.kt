package app.katari.sync.protocol.link

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class SyncLinksTest {

    @Test
    fun `values with separators, plus signs and non-ASCII text come back as written`() {
        val values = listOf(
            "server" to "https://sync.example.com:8443/katari/?a=1&b=2",
            "code" to "DD01-P9BM",
            "from" to "Kiryl's Pixel + Tab = ✓ 🙂 50%",
        )

        SyncLinks.parameters(SyncLinks.encode("link", values), "link") shouldBe values.toMap()
    }

    @Test
    fun `a link as earlier apps wrote it through OkHttp reads the same`() {
        val written = "katari-sync:link?server=https%3A%2F%2Fsync.example.com%3A8443%2F&code=DD01-P9BM" +
            "&account=kiryl&from=Kiryl%27s%20Pixel%20%2B%20Tab%20%E2%9C%93*&later=ignored"

        SyncLinks.parameters(written, "link") shouldBe mapOf(
            "server" to "https://sync.example.com:8443/",
            "code" to "DD01-P9BM",
            "account" to "kiryl",
            "from" to "Kiryl's Pixel + Tab ✓*",
            "later" to "ignored",
        )
        SyncLinks.parameters(written, "invite") shouldBe null
    }
}
