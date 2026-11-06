package app.katari.sync.server.account

import app.katari.sync.server.attemptlimit.AttemptLimiter
import app.katari.sync.server.storage.ServerStorage
import kotlin.time.Duration

/**
 * Checks a username and password someone typed, wherever they type one: device and web sign-in, and the web page's
 * own password prompts. Failures count against the account and the remote address alike, so guessing one account is
 * limited however many addresses it comes from.
 */
class PasswordCheck(
    private val storage: ServerStorage,
    private val passwords: PasswordHasher,
    private val attempts: AttemptLimiter,
) {
    fun check(username: String, password: String, remoteAddress: String): PasswordCheckOutcome {
        val keys = listOf(AttemptLimiter.passwordsFrom(remoteAddress), AttemptLimiter.account(username))
        attempts.blockedFor(keys)?.let { return PasswordCheckOutcome.Throttled(it) }
        val account = storage.accounts.byUsername(username.trim())
        if (account == null) passwords.verifyNothing(password)
        if (account == null || !passwords.verify(password, account.passwordHash)) {
            attempts.failed(keys)
            return PasswordCheckOutcome.Wrong
        }
        attempts.succeeded(keys)
        return PasswordCheckOutcome.Matched(account)
    }
}

sealed interface PasswordCheckOutcome {
    /** Also for a disabled account: whether it may do anything is for the caller to decide. */
    data class Matched(val account: Account) : PasswordCheckOutcome

    /** A wrong password or an unknown username, which look the same from outside. */
    data object Wrong : PasswordCheckOutcome

    /** Too many failures for this account or from this address; the password was not checked. */
    data class Throttled(val retryAfter: Duration) : PasswordCheckOutcome
}
