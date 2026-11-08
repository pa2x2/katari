package app.katari.sync.server.websession

import app.katari.sync.server.account.Account
import app.katari.sync.server.storage.ServerStorage
import app.katari.sync.server.token.BearerTokens
import java.security.SecureRandom
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days

/**
 * Web page sign-ins, kept on the server so signing out, a password change or reset, and disabling or deleting the
 * account end them at once. A browser holds only the session's token, which is nothing like a device's: a web session
 * can't sync and a device token can't open the web page.
 */
class WebSessions(
    private val storage: ServerStorage,
    private val clock: Clock,
    private val random: SecureRandom,
) {
    /** The new session's token, for the browser's cookie. */
    fun start(accountId: Long): String {
        val token = BearerTokens.generate(random)
        val expiresAt = clock.now().plus(LIFETIME).toEpochMilliseconds()
        storage.webSessions.insert(BearerTokens.hash(token), accountId, expiresAt)
        return token
    }

    /** The account signed in with [token], or null when the session ended or its account is disabled or gone. */
    fun accountOf(token: String): Account? = storage.transaction {
        storage.webSessions.accountOf(BearerTokens.hash(token), clock.now().toEpochMilliseconds())
            ?.let(storage.accounts::byId)
            ?.takeUnless { it.isDisabled }
    }

    fun end(token: String) {
        storage.webSessions.delete(BearerTokens.hash(token))
    }

    fun forgetExpired() {
        storage.webSessions.deleteExpired(clock.now().toEpochMilliseconds())
    }

    companion object {
        /** From sign-in, however often the page is used. */
        val LIFETIME: Duration = 30.days
    }
}
