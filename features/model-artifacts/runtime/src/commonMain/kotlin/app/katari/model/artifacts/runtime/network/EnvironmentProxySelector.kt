package app.katari.model.artifacts.runtime.network

import java.io.IOException
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.ProxySelector
import java.net.SocketAddress
import java.net.URI

/**
 * Sends requests through the proxy the process's environment names, the way command line tools read it: `https_proxy`
 * and `http_proxy` for their schemes, `all_proxy` for both, each also in capitals, and `no_proxy` for the hosts that go
 * direct. A proxy is `host:port` or a URL, a `socks` one for a SOCKS proxy; a name and password in it aren't sent. With
 * no proxy named for a request, [fallback] decides.
 *
 * Java reads none of these variables itself, so without this a download ignores the proxy the rest of a user's tools
 * go through.
 */
internal class EnvironmentProxySelector(
    private val environment: (String) -> String?,
    private val fallback: ProxySelector?,
) : ProxySelector() {
    override fun select(uri: URI): List<Proxy> {
        val host = uri.host
        val proxy = if (host == null || goesDirect(host)) null else proxyFor(uri.scheme)
        return proxy?.let(::listOf) ?: fallback?.select(uri) ?: listOf(Proxy.NO_PROXY)
    }

    override fun connectFailed(uri: URI, address: SocketAddress, error: IOException) {
        fallback?.connectFailed(uri, address, error)
    }

    private fun proxyFor(scheme: String?): Proxy? {
        val names = when (scheme?.lowercase()) {
            "https" -> listOf("https_proxy", "HTTPS_PROXY")
            "http" -> listOf("http_proxy", "HTTP_PROXY")
            else -> return null
        } + listOf("all_proxy", "ALL_PROXY")
        return names.firstNotNullOfOrNull { name -> environment(name)?.trim()?.takeIf(String::isNotEmpty) }
            ?.let(::parse)
    }

    private fun parse(value: String): Proxy? {
        val uri = runCatching { URI(if ("://" in value) value else "http://$value") }.getOrNull() ?: return null
        val host = uri.host ?: return null
        val socks = uri.scheme.startsWith("socks", ignoreCase = true)
        val port = uri.port.takeIf { it > 0 } ?: if (socks) SOCKS_PORT else HTTP_PORT
        return Proxy(if (socks) Proxy.Type.SOCKS else Proxy.Type.HTTP, InetSocketAddress.createUnresolved(host, port))
    }

    private fun goesDirect(host: String): Boolean {
        val exempt = (environment("no_proxy") ?: environment("NO_PROXY")).orEmpty()
        return exempt.split(',').map { it.trim().removePrefix(".").lowercase() }.filter(String::isNotEmpty).any {
            it == "*" || host.equals(it, ignoreCase = true) || host.endsWith(".$it", ignoreCase = true)
        }
    }

    private companion object {
        const val HTTP_PORT = 80
        const val SOCKS_PORT = 1080
    }
}
