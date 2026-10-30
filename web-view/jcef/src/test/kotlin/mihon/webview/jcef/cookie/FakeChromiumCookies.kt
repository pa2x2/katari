package mihon.webview.jcef.cookie

import kotlin.time.Duration.Companion.days

/**
 * Chromium's cookie manager as the mirror meets it: one cookie per key, expiries kept in whole seconds and capped at
 * 400 days, prefixed names refused without `Secure`, and an expired cookie removing the one it replaces.
 */
internal class FakeChromiumCookies : ChromiumCookieManager {
    private val cookies = LinkedHashMap<ChromiumCookie.Key, ChromiumCookie>()

    var sets = 0
        private set

    val held: List<ChromiumCookie>
        get() = synchronized(cookies) { cookies.values.toList() }

    override fun set(cookie: ChromiumCookie) {
        synchronized(cookies) {
            sets++
            store(cookie)
        }
    }

    override fun remove(cookie: ChromiumCookie) {
        synchronized(cookies) { cookies.remove(cookie.key) }
    }

    override fun removeAll() {
        synchronized(cookies) { cookies.clear() }
    }

    override suspend fun read(): List<ChromiumCookie> = held

    /** A page, or the response of one, setting [cookie]. */
    fun setByPage(cookie: ChromiumCookie) {
        synchronized(cookies) { store(cookie) }
    }

    private fun store(cookie: ChromiumCookie) {
        if (PREFIXES.any(cookie.name::startsWith) && !cookie.secure) return
        val now = System.currentTimeMillis()
        val expiresAt = cookie.expiresAt?.let { minOf(it, now + MAX_AGE) / 1000 * 1000 }
        cookies.remove(cookie.key)
        if (expiresAt == null || expiresAt > now) cookies[cookie.key] = cookie.copy(expiresAt = expiresAt)
    }

    private companion object {
        val PREFIXES = listOf("__Host-", "__Secure-")
        val MAX_AGE = 400.days.inWholeMilliseconds
    }
}
