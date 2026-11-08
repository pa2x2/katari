package eu.kanade.tachiyomi.network

import app.katari.core.common.network.cookie.AppCookieStore
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

/**
 * A cookie jar over [AppCookieStore], the app's lasting store, as Android's jar is over WebView's `CookieManager`:
 * every jar, however it was built, sees the same cookies, and Chromium's are kept in step with them.
 */
actual class AndroidCookieJar actual constructor() : CookieJar {

    private val store = AppCookieStore.store

    actual override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        store.save(cookies)
    }

    actual override fun loadForRequest(url: HttpUrl): List<Cookie> = get(url)

    actual fun get(url: HttpUrl): List<Cookie> = store.matching(url)

    actual fun remove(url: HttpUrl, cookieNames: List<String>?, maxAge: Int): Int = store.remove(url, cookieNames)

    actual fun removeAll() {
        store.removeAll()
    }
}
