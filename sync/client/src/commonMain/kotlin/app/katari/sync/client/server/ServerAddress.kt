package app.katari.sync.client.server

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.net.InetAddress

/**
 * A sync server's address as the person typed it, checked before anything is sent there (D027). A missing scheme
 * means HTTPS. Plain `http://` is accepted only on a local network, where a home server often has no certificate,
 * and [ServerAddress.Accepted.isPlainHttp] then says so for the screen to warn: on any other network a password and
 * every synced record would cross the internet readable.
 */
sealed interface ServerAddress {
    data class Accepted(val url: HttpUrl) : ServerAddress {
        val isPlainHttp: Boolean get() = !url.isHttps
    }

    /** Not an address at all. */
    data object Malformed : ServerAddress

    /** Plain HTTP to a host that isn't on a local network. */
    data object PlainHttpOutsideLocalNetwork : ServerAddress

    companion object {
        fun parse(input: String): ServerAddress {
            val trimmed = input.trim().trimEnd('/')
            if (trimmed.isEmpty()) return Malformed
            val withScheme = if ("://" in trimmed) trimmed else "https://$trimmed"
            val url = withScheme.toHttpUrlOrNull() ?: return Malformed
            if (url.query != null || url.fragment != null) return Malformed
            if (!url.isHttps && !isLocalNetworkHost(url.host)) return PlainHttpOutsideLocalNetwork
            return Accepted(url)
        }

        /**
         * Hosts a home network names or addresses: private, loopback, link-local and unique local addresses, and the
         * names local resolvers answer for (single-label names, `.local`, `.lan`, `.home.arpa`, `.internal`). Names
         * are judged by their shape alone, never by resolving them, so typing an address never sends a query.
         */
        internal fun isLocalNetworkHost(host: String): Boolean {
            val name = host.lowercase().trimEnd('.')
            literalAddress(name)?.let { address ->
                return address.isLoopbackAddress || address.isSiteLocalAddress || address.isLinkLocalAddress ||
                    (address.address.size == IPV6_BYTES && address.address[0].toInt() and 0xFE == 0xFC)
            }
            return name == "localhost" || '.' !in name || LOCAL_SUFFIXES.any { name.endsWith(it) }
        }

        // An IP literal is parsed without a lookup; anything else isn't one.
        private fun literalAddress(host: String): InetAddress? {
            val isLiteral = ':' in host || IPV4.matches(host)
            return if (isLiteral) runCatching { InetAddress.getByName(host) }.getOrNull() else null
        }

        private const val IPV6_BYTES = 16
        private val IPV4 = Regex("""\d{1,3}(\.\d{1,3}){3}""")
        private val LOCAL_SUFFIXES = listOf(".local", ".lan", ".home.arpa", ".internal", ".localhost")
    }
}
