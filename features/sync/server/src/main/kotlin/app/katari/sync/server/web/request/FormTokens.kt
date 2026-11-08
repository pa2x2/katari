package app.katari.sync.server.web.request

import app.katari.sync.server.token.BearerTokens
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Cross-site request forgery protection: every state-changing form carries a token that a page on another site can't
 * know. A signed-in visitor's token is derived from their session token, which only their browser's HttpOnly cookie
 * holds, so it changes with every sign-in and dies with the session. A visitor not signed in (setting up, signing in)
 * gets a random one in a cookie of its own, matched against the form's copy. Both cookies are SameSite=Strict, so a
 * cross-site POST also arrives without them.
 */
internal object FormTokens {
    const val FIELD = "form_token"

    fun forSession(sessionToken: String): String =
        MessageDigest.getInstance("SHA-256").digest("katari-form\u0000$sessionToken".encodeToByteArray()).toHexString()

    fun newAnonymous(random: SecureRandom): String = BearerTokens.generate(random)

    fun matches(sent: String?, expected: String): Boolean =
        sent != null && MessageDigest.isEqual(sent.encodeToByteArray(), expected.encodeToByteArray())
}
