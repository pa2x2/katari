package app.katari.extension.runtime.jvm.cookie

import app.katari.core.common.network.cookie.DesktopCookieStore
import app.katari.extension.compat.WebViewCookies
import okhttp3.Cookie
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/**
 * Extensions' `CookieManager` on the app's cookie [store], read and written as Android's WebView reads and writes its
 * own: a cookie is parsed as a response from its URL would set it, and the `Cookie` line lists the cookies with longer
 * paths first.
 */
class DesktopWebViewCookies(private val store: DesktopCookieStore) : WebViewCookies {

    override fun setCookie(url: String, setCookie: String): Boolean {
        val httpUrl = url.toHttpUrlOrNull() ?: return false
        val cookie = Cookie.parse(httpUrl, setCookie) ?: return false
        // WebView refuses these for apps targeting Android 9 or later; OkHttp would keep them
        if (cookie.secure && !httpUrl.isHttps) return false
        store.save(listOf(cookie))
        return true
    }

    override fun getCookie(url: String): String? {
        val httpUrl = url.toHttpUrlOrNull() ?: return null
        return store.matching(httpUrl)
            .sortedByDescending { it.path.length }
            .takeIf { it.isNotEmpty() }
            ?.joinToString("; ") { "${it.name}=${it.value}" }
    }

    override fun hasCookies(): Boolean = store.all().isNotEmpty()

    override fun removeAllCookies(): Boolean = store.removeAll()

    override fun removeSessionCookies(): Boolean = store.removeSessionCookies()

    override fun flush() {
        store.flush()
    }
}
