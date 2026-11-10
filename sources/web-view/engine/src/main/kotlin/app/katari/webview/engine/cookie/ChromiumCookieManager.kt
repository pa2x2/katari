package app.katari.webview.engine.cookie

/**
 * Chromium's cookies, as the [ChromiumCookieMirror] changes and reads them. Changes don't wait for Chromium and take
 * effect in the order they're made; [read] sees every change made before it.
 */
interface ChromiumCookieManager {
    /** Sets [cookie] in place of the one with its [ChromiumCookie.key]. */
    fun set(cookie: ChromiumCookie)

    /**
     * Removes the cookie with [cookie]'s [ChromiumCookie.key], and with it every other cookie of its name that a request
     * to its host and path carries ([ChromiumCookie.isSentWith]), as Chromium removes cookies only by address and name.
     */
    fun remove(cookie: ChromiumCookie)

    fun removeAll()

    /** Every cookie Chromium holds; null where Chromium didn't answer in time. */
    suspend fun read(): List<ChromiumCookie>?
}
