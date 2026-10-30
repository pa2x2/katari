package mihon.webview.jcef.cookie

/**
 * Chromium's cookies, as the [ChromiumCookieMirror] changes and reads them. Changes don't wait for Chromium and take
 * effect in the order they're made; [read] sees every change made before it.
 */
internal interface ChromiumCookieManager {
    /** Sets [cookie] in place of the one with its [ChromiumCookie.key]. */
    fun set(cookie: ChromiumCookie)

    /** Removes the cookie with [cookie]'s [ChromiumCookie.key]. */
    fun remove(cookie: ChromiumCookie)

    fun removeAll()

    /** Every cookie Chromium holds; null where Chromium didn't answer in time. */
    suspend fun read(): List<ChromiumCookie>?
}
