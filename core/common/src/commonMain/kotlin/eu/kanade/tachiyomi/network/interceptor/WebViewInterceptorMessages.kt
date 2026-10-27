package eu.kanade.tachiyomi.network.interceptor

/**
 * What the WebView interceptors tell the user. The host app words it in the user's language, so the published SDK
 * carries no translations; each is read when it's shown.
 */
interface WebViewInterceptorMessages {
    /** Shown when a response needs a WebView to get through and the device has none. */
    fun webViewRequired(): String

    /** Shown when a Cloudflare challenge went unsolved in a WebView that is out of date. */
    fun webViewOutdated(): String

    /** The message of the IOException thrown when a Cloudflare challenge couldn't be solved. */
    fun cloudflareBypassFailed(): String
}
