package app.katari.model.artifacts.runtime.network

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.URI

class EnvironmentProxySelectorTest {
    private fun select(url: String, vararg environment: Pair<String, String>) =
        EnvironmentProxySelector(environment.toMap()::get, fallback = null).select(URI(url)).single()

    @Test
    fun `a request goes through the proxy named for its scheme`() {
        val proxy = select("https://github.com/a", "https_proxy" to "http://proxy.lan:3128", "all_proxy" to "other:1")

        proxy shouldBe Proxy(Proxy.Type.HTTP, InetSocketAddress.createUnresolved("proxy.lan", 3128))
    }

    @Test
    fun `a socks proxy for every scheme is used as one`() {
        val proxy = select("https://github.com/a", "ALL_PROXY" to "socks5://127.0.0.1")

        proxy shouldBe Proxy(Proxy.Type.SOCKS, InetSocketAddress.createUnresolved("127.0.0.1", 1080))
    }

    @Test
    fun `a host the environment exempts goes direct`() {
        val proxy = select(
            "https://objects.github.com/a",
            "https_proxy" to "proxy.lan:3128",
            "no_proxy" to ".github.com",
        )

        proxy shouldBe Proxy.NO_PROXY
    }
}
