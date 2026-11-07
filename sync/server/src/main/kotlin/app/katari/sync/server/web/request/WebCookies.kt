package app.katari.sync.server.web.request

import app.katari.sync.server.http.cameOverHttps
import app.katari.sync.server.websession.WebSessions
import io.ktor.http.Cookie
import io.ktor.server.application.ApplicationCall
import kotlin.time.Duration

/**
 * The web page's cookies: the session and the signed-out form token ([FormTokens]). Both are HttpOnly, since no script
 * reads them, SameSite=Strict, and Secure when the browser came over HTTPS, which only a trusted proxy can tell.
 */
internal object WebCookies {
    const val SESSION = "katari_session"
    const val FORM = "katari_form"

    fun setSession(call: ApplicationCall, token: String, trustedProxies: Int) {
        call.set(SESSION, token, WebSessions.LIFETIME, trustedProxies)
    }

    fun clearSession(call: ApplicationCall, trustedProxies: Int) {
        call.set(SESSION, "", Duration.ZERO, trustedProxies)
    }

    /** Lives as long as the browser session; it guards only the forms shown before signing in. */
    fun setForm(call: ApplicationCall, token: String, trustedProxies: Int) {
        call.set(FORM, token, maxAge = null, trustedProxies)
    }

    private fun ApplicationCall.set(name: String, value: String, maxAge: Duration?, trustedProxies: Int) {
        response.cookies.append(
            Cookie(
                name = name,
                value = value,
                maxAge = maxAge?.inWholeSeconds?.toInt(),
                path = "/",
                secure = cameOverHttps(trustedProxies),
                httpOnly = true,
                extensions = mapOf("SameSite" to "Strict"),
            ),
        )
    }
}
