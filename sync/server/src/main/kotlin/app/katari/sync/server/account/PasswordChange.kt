package app.katari.sync.server.account

import app.katari.sync.protocol.account.AccountCredentials
import app.katari.sync.server.storage.ServerStorage
import kotlin.time.Duration

/** A person changing their own password, which ends every web session of the account; devices keep their tokens. */
class PasswordChange(
    private val storage: ServerStorage,
    private val passwords: PasswordHasher,
    private val passwordCheck: PasswordCheck,
) {
    fun change(account: Account, current: String, new: String, remoteAddress: String): PasswordChangeOutcome {
        when (val check = passwordCheck.check(account.username, current, remoteAddress)) {
            is PasswordCheckOutcome.Matched ->
                if (check.account.id != account.id) return PasswordChangeOutcome.WrongPassword
            PasswordCheckOutcome.Wrong -> return PasswordChangeOutcome.WrongPassword
            is PasswordCheckOutcome.Throttled -> return PasswordChangeOutcome.Throttled(check.retryAfter)
        }
        if (!AccountCredentials.isValidPassword(new)) return PasswordChangeOutcome.InvalidPassword
        val hash = passwords.hash(new)
        storage.transaction {
            storage.accounts.setPasswordHash(account.id, hash)
            storage.webSessions.deleteForAccount(account.id)
        }
        return PasswordChangeOutcome.Changed
    }
}

sealed interface PasswordChangeOutcome {
    data object Changed : PasswordChangeOutcome

    data object WrongPassword : PasswordChangeOutcome

    data object InvalidPassword : PasswordChangeOutcome

    data class Throttled(val retryAfter: Duration) : PasswordChangeOutcome
}
