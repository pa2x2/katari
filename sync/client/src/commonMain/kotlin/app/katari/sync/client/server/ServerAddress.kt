package app.katari.sync.client.server

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.net.InetAddress

/**
 * A sync server's address as the person typed it, checked before anything is sent there (D027). A missing scheme
 * means HTTPS, and on a local network plain HTTP after it: a home server often has no certificate, and its
 * address is typed bare. Plain `http://` is accepted only on a local network, and the screen warns when a server is
 * reached that way: on any other network a password and every synced record would cross the internet readable.
 */
sealed interface ServerAddress {
    /**
     * [url] as typed, or over HTTPS when typed without a scheme. [plainHttpFallback] is the same address over plain
     * HTTP, for a local-network address typed without a scheme, to try when no sync server answers at [url].
     */
    data class Accepted(val url: HttpUrl, val plainHttpFallback: HttpUrl?) : ServerAddress

    /** Not an address at all. */
    data object Malformed : ServerAddress

    /** Plain HTTP to a host that isn't on a local network. */
    data object PlainHttpOutsideLocalNetwork : ServerAddress

    companion object {
        fun parse(input: String): ServerAddress {
            val trimmed = input.trim().trimEnd('/')
            if (trimmed.isEmpty()) return Malformed
            val hasScheme = "://" in trimmed
            val url = (if (hasScheme) trimmed else "https://$trimmed").toHttpUrlOrNull() ?: return Malformed
            if (url.query != null || url.fragment != null) return Malformed
            val isLocal = isLocalNetworkHost(url.host)
            if (!url.isHttps && !isLocal) return PlainHttpOutsideLocalNetwork
            val fallback = if (!hasScheme && isLocal) url.newBuilder().scheme("http").build() else null
            return Accepted(url, fallback)
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
