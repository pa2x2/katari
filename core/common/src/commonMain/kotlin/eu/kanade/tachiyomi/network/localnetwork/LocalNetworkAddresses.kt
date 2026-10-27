package eu.kanade.tachiyomi.network.localnetwork

import java.net.Inet6Address
import java.net.InetAddress

/**
 * Whether Android's local network protection gates traffic to this address: private, link-local, unique local and
 * multicast destinations. Loopback stays reachable, and carrier-grade NAT space (also used by mesh VPNs) is routed
 * rather than on-link, so it is treated as internet traffic.
 */
internal fun InetAddress.isLocalNetworkAddress(): Boolean {
    if (isLoopbackAddress || isAnyLocalAddress) return false
    if (isSiteLocalAddress || isLinkLocalAddress || isMulticastAddress) return true
    return when (this) {
        is Inet6Address -> address[0].toInt() and 0xFE == 0xFC
        else -> address.all { it == 0xFF.toByte() }
    }
}

/** Multicast DNS names resolve by querying the local network, which local network protection gates as well. */
internal fun isMulticastDnsHostName(hostname: String): Boolean {
    return hostname.trimEnd('.').endsWith(".local", ignoreCase = true)
}
