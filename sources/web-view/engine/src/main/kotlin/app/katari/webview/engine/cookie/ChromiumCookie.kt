package app.katari.webview.engine.cookie

import okhttp3.Cookie

/**
 * A cookie as JCEF hands Chromium's over, which carries no `SameSite`, priority or partition: a domain cookie's
 * [domain] starts with a dot, a host-only cookie's is its host. Chromium holds one cookie per [key], so a host-only and
 * a domain cookie of the same name and path are two, where the app's store keeps one.
 *
 * @property expiresAt milliseconds since the epoch; null for a session cookie. Chromium keeps at most 400 days from
 * when the cookie was set, and of a cookie set through JCEF only whole seconds.
 */
data class ChromiumCookie(
    val name: String,
    val value: String,
    val domain: String,
    val path: String,
    val secure: Boolean,
    val httpOnly: Boolean,
    val expiresAt: Long?,
) {
    val key: Key
        get() = Key(name, domain, path)

    val host: String
        get() = domain.removePrefix(DOMAIN_COOKIE_PREFIX)

    data class Key(val name: String, val domain: String, val path: String)

    /**
     * Whether [seen], read back from Chromium, is still this cookie as it was set: the same but for an expiry Chromium
     * rounded down or capped. A page that set it again with a later expiry or another value has changed it.
     */
    fun isHeldAs(seen: ChromiumCookie): Boolean {
        if (seen.copy(expiresAt = expiresAt) != this) return false
        return when (expiresAt) {
            null -> seen.expiresAt == null
            else -> seen.expiresAt != null && seen.expiresAt <= expiresAt
        }
    }

    /** Whether a request to [other]'s host and path carries this cookie too (RFC 6265 domain and path match). */
    fun isSentWith(other: ChromiumCookie): Boolean {
        val domainMatches = if (domain.startsWith(DOMAIN_COOKIE_PREFIX)) {
            other.host == host || other.host.endsWith(domain)
        } else {
            other.host == host
        }
        val pathMatches = other.path == path ||
            (other.path.startsWith(path) && (path.endsWith('/') || other.path[path.length] == '/'))
        return domainMatches && pathMatches
    }

    /** The cookie for the app's store, without the `SameSite` Chromium doesn't tell; null where OkHttp refuses it. */
    fun toCookie(): Cookie? = try {
        Cookie.Builder()
            .name(name)
            .value(value)
            .apply { if (domain.startsWith(DOMAIN_COOKIE_PREFIX)) domain(host) else hostOnlyDomain(host) }
            .path(path)
            .apply {
                expiresAt?.let(::expiresAt)
                if (secure) secure()
                if (httpOnly) httpOnly()
            }
            .build()
    } catch (_: IllegalArgumentException) {
        null
    }

    private companion object {
        const val DOMAIN_COOKIE_PREFIX = "."
    }
}

/** The cookie as Chromium is to hold it; Chromium gives it the `SameSite` of a cookie set without one. */
fun Cookie.toChromiumCookie() = ChromiumCookie(
    name = name,
    value = value,
    domain = if (hostOnly) domain else ".$domain",
    path = path,
    secure = secure,
    httpOnly = httpOnly,
    expiresAt = expiresAt.takeIf { persistent },
)
