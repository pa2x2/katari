package app.katari.extension.compat

/**
 * The app's cookie store as `android.webkit.CookieManager` shows it to extensions: the store the app's HTTP clients and
 * web view use, so a cookie an extension sets there is sent with their requests, and the other way round. URLs come
 * as `CookieManager` normalises them, with a scheme and a path.
 */
interface WebViewCookies {
    /**
     * Stores [setCookie], a `Set-Cookie` header value, as a response from [url] would, replacing the cookie with its
     * name, domain and path; an expired cookie only removes that one. False, and nothing stored, where it isn't a
     * cookie [url] may set: malformed, for another site, or `Secure` from a URL that isn't HTTPS.
     */
    fun setCookie(url: String, setCookie: String): Boolean

    /** The `Cookie` header a request to [url] carries, `name=value` pairs joined by `"; "`; null where it has none. */
    fun getCookie(url: String): String?

    fun hasCookies(): Boolean

    /** Whether there were any to remove. */
    fun removeAllCookies(): Boolean

    /** Removes the cookies without an expiry; whether there were any. */
    fun removeSessionCookies(): Boolean

    /** Writes every change to disk, blocking until it is written. */
    fun flush()
}
