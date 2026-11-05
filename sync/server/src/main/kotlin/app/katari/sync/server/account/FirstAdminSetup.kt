package app.katari.sync.server.account

import app.katari.sync.server.attemptlimit.AttemptLimiter
import app.katari.sync.server.onetimecode.OneTimeCode
import app.katari.sync.server.storage.ServerStorage
import java.security.SecureRandom
import java.util.concurrent.atomic.AtomicReference
import kotlin.time.Clock
import kotlin.time.Duration

/**
 * Creates the first admin account while the server has none. So that whoever reaches the address first cannot claim
 * the server, it takes a one-time [setupCode] made when the server starts without accounts and written to its log,
 * which only whoever runs the server can read. A restart before setup makes a new code. Wrong codes are limited per
 * remote address.
 */
class FirstAdminSetup(
    private val storage: ServerStorage,
    private val passwords: PasswordHasher,
    private val clock: Clock,
    random: SecureRandom,
    private val attempts: AttemptLimiter,
) {
    private val code = AtomicReference(
        if (storage.accounts.count() == 0L) OneTimeCode.generate(SETUP_CODE_LENGTH, random) else null,
    )

    /** Null once the server has an account. */
    val setupCode: String?
        get() = code.get()

    fun createAdmin(setupCode: String, username: String, password: String, remoteAddress: String): FirstAdminOutcome {
        val expected = code.get() ?: return FirstAdminOutcome.AlreadySetUp
        val keys = listOf(AttemptLimiter.address(remoteAddress))
        attempts.blockedFor(keys)?.let { return FirstAdminOutcome.Throttled(it) }
        if (!OneTimeCode.matches(setupCode, expected)) {
            attempts.failed(keys)
            return FirstAdminOutcome.WrongCode
        }
        attempts.succeeded(keys)
        val name = username.trim()
        if (!AccountCredentials.isValidUsername(name)) return FirstAdminOutcome.InvalidUsername
        if (!AccountCredentials.isValidPassword(password)) return FirstAdminOutcome.InvalidPassword
        val hash = passwords.hash(password)
        val account = storage.transaction {
            if (storage.accounts.count() > 0) {
                null
            } else {
                storage.accounts.create(name, hash, isAdmin = true, createdAtMillis = clock.now().toEpochMilliseconds())
            }
        }
        code.set(null)
        return account?.let(FirstAdminOutcome::Created) ?: FirstAdminOutcome.AlreadySetUp
    }

    private companion object {
        const val SETUP_CODE_LENGTH = 12
    }
}

sealed interface FirstAdminOutcome {
    data class Created(val account: Account) : FirstAdminOutcome

    data object WrongCode : FirstAdminOutcome

    /** Too many wrong codes from this address; the code was not checked. */
    data class Throttled(val retryAfter: Duration) : FirstAdminOutcome

    data object AlreadySetUp : FirstAdminOutcome

    data object InvalidUsername : FirstAdminOutcome

    data object InvalidPassword : FirstAdminOutcome
}
