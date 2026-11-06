package app.katari.sync.server.recovery

import app.katari.sync.server.account.GeneratedPasswords
import app.katari.sync.server.account.PasswordHasher
import app.katari.sync.server.storage.ServerStorage
import java.security.SecureRandom

/**
 * The recovery command for an admin locked out of the web page: gives the account a new random password and enables
 * it if it was disabled. It runs inside the container, so whoever can run it already controls the server.
 */
class PasswordReset(
    private val storage: ServerStorage,
    private val passwords: PasswordHasher,
    private val random: SecureRandom,
) {
    /** The new password, or null when no account has [username]. */
    fun reset(username: String): String? {
        val account = storage.accounts.byUsername(username.trim()) ?: return null
        val password = GeneratedPasswords.generate(random)
        val hash = passwords.hash(password)
        storage.transaction {
            storage.accounts.setPasswordHash(account.id, hash)
            storage.accounts.setDisabled(account.id, disabled = false)
            storage.webSessions.deleteForAccount(account.id)
        }
        return password
    }
}
