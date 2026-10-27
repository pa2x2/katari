package eu.kanade.tachiyomi.network

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

/** The cookie store shared by the HTTP clients and the web view. */
expect class AndroidCookieJar() : CookieJar {
    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>)

    override fun loadForRequest(url: HttpUrl): List<Cookie>

    fun get(url: HttpUrl): List<Cookie>

    fun remove(url: HttpUrl, cookieNames: List<String>? = null, maxAge: Int = -1): Int

    fun removeAll()
}
