package eu.kanade.tachiyomi.network

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

/** Kept in memory until desktop has a web view whose cookie store both can share. */
actual class AndroidCookieJar actual constructor() : CookieJar {

    private val cookies = mutableListOf<Cookie>()

    actual override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        synchronized(this.cookies) {
            cookies.forEach { cookie ->
                this.cookies.removeAll {
                    it.name == cookie.name && it.domain == cookie.domain && it.path == cookie.path
                }
                if (cookie.expiresAt > System.currentTimeMillis()) this.cookies += cookie
            }
        }
    }

    actual override fun loadForRequest(url: HttpUrl): List<Cookie> = get(url)

    actual fun get(url: HttpUrl): List<Cookie> = synchronized(cookies) {
        val now = System.currentTimeMillis()
        cookies.removeAll { it.expiresAt <= now }
        cookies.filter { it.matches(url) }
    }

    actual fun remove(url: HttpUrl, cookieNames: List<String>?, maxAge: Int): Int = synchronized(cookies) {
        val removed = cookies.filter { it.matches(url) && (cookieNames == null || it.name in cookieNames) }
        cookies.removeAll(removed)
        removed.size
    }

    actual fun removeAll() {
        synchronized(cookies) { cookies.clear() }
    }
}
