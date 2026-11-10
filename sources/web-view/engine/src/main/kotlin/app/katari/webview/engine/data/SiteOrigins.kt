package app.katari.webview.engine.data

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.util.concurrent.ConcurrentHashMap

/**
 * The web origins Chromium's pages loaded in this run. Chromium keeps what it read of a site's storage in memory once
 * a page of the site used it, and clears it only for origins it is named, as it has no call that clears every site.
 */
class SiteOrigins {
    private val origins = ConcurrentHashMap.newKeySet<String>()

    /** A frame loaded [url]. */
    fun loaded(url: String) {
        url.toHttpUrlOrNull()?.let { origins += it.origin() }
    }

    fun all(): Set<String> = origins.toSet()

    private fun HttpUrl.origin(): String {
        val host = if (':' in host) "[$host]" else host
        val port = if (port == HttpUrl.defaultPort(scheme)) "" else ":$port"
        return "$scheme://$host$port"
    }
}
