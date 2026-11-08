package app.katari.sync.server.web.request

import io.ktor.http.HttpHeaders
import io.ktor.server.application.ApplicationCall

/**
 * Headers every web page carries. The pages load nothing but the server's own stylesheet, script, font and logo, so
 * the content policy allows nothing else, and no other site may frame them. Pages can show a one-time password or
 * code, so nothing may cache them or send their address on.
 */
internal object WebSecurityHeaders {
    private const val CONTENT_SECURITY_POLICY = "default-src 'none'; style-src 'self'; script-src 'self'; " +
        "font-src 'self'; img-src 'self'; connect-src 'none'; form-action 'self'; frame-ancestors 'none'; " +
        "base-uri 'none'"

    fun apply(call: ApplicationCall) {
        with(call.response.headers) {
            append("Content-Security-Policy", CONTENT_SECURITY_POLICY)
            append(HttpHeaders.CacheControl, "no-store")
            append("Referrer-Policy", "no-referrer")
            append("X-Content-Type-Options", "nosniff")
            append("X-Frame-Options", "DENY")
        }
    }
}
