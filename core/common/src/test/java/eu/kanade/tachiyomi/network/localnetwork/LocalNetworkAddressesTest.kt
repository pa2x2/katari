package eu.kanade.tachiyomi.network.localnetwork

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.net.InetAddress

class LocalNetworkAddressesTest {

    @Test
    fun `only destinations that local network protection blocks are gated`() {
        val local = listOf(
            "192.168.1.20",
            "10.0.0.5",
            "172.31.255.1",
            "169.254.10.10",
            "224.0.0.251",
            "255.255.255.255",
            "fe80::1",
            "fd7a:115c:a1e0::1",
            "ff02::fb",
            "::ffff:192.168.1.20",
        )
        val unrestricted = listOf(
            "127.0.0.1",
            "::1",
            "0.0.0.0",
            "8.8.8.8",
            "172.32.0.1",
            "100.100.1.1",
            "2606:4700::1111",
        )

        local.filterNot { InetAddress.getByName(it).isLocalNetworkAddress() } shouldBe emptyList()
        unrestricted.filter { InetAddress.getByName(it).isLocalNetworkAddress() } shouldBe emptyList()
    }
}
