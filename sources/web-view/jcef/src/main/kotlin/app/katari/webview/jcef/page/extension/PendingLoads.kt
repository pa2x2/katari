package app.katari.webview.jcef.page.extension

import app.katari.core.common.web.ExtensionPageResponse
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/**
 * What the loads an extension starts carry beyond their URL, until the main frame's request for that URL comes: the
 * headers of `loadUrl(url, headers)`, which go with that request only, as Android's web view sends them, and the
 * document `loadDataWithBaseURL` shows at its base URL, which answers that request instead of the network.
 *
 * A load waits for its own URL, so a navigation the page starts meanwhile gets neither; a later load of the same kind
 * replaces an earlier one that never came. Read on Chromium's threads, written on the page's.
 */
internal class PendingLoads {
    private var headers: Pair<String, Map<String, String>>? = null
    private var content: Pair<String, () -> ExtensionPageResponse>? = null

    @Synchronized
    fun expectHeaders(url: String, headers: Map<String, String>) {
        this.headers = if (headers.isEmpty()) null else key(url) to headers
    }

    @Synchronized
    fun expectContent(url: String, response: () -> ExtensionPageResponse) {
        content = key(url) to response
    }

    /** The headers for the main frame's request of [url], once. */
    @Synchronized
    fun takeHeaders(url: String): Map<String, String>? {
        val (expected, headers) = this.headers ?: return null
        if (expected != key(url)) return null
        this.headers = null
        return headers
    }

    /** The document that answers the main frame's request of [url], once. */
    @Synchronized
    fun takeContent(url: String): ExtensionPageResponse? {
        val (expected, response) = content ?: return null
        if (expected != key(url)) return null
        content = null
        return response()
    }

    // Chromium asks for a URL as it normalised it, such as with the path "/" an origin's URL lacks
    private fun key(url: String): String = url.toHttpUrlOrNull()?.toString() ?: url
}
