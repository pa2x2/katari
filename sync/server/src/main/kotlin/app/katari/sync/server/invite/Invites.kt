package app.katari.sync.server.invite

import app.katari.sync.protocol.account.AccountCredentials
import app.katari.sync.protocol.account.RedeemInviteRequest
import app.katari.sync.protocol.device.RegisterDeviceResponse
import app.katari.sync.server.account.PasswordHasher
import app.katari.sync.server.administration.enabledAdmin
import app.katari.sync.server.attemptlimit.AttemptLimiter
import app.katari.sync.server.device.DeviceRegistration
import app.katari.sync.server.onetimecode.OneTimeCode
import app.katari.sync.server.storage.ServerStorage
import java.security.SecureRandom
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days

/**
 * One-time invites: an admin makes one on the web page, and the invited person enters it in the app with the username
 * and password they choose, which creates their account and links that device in one step. The server keeps only a
 * code's hash. A code is used up when its account is created, not by a refused attempt, so a person whose username is
 * taken can pick another with the same code.
 *
 * Codes are 8 characters of Crockford Base32, 40 bits, in the form `QX7P-M42K`. Their guesses are limited per address
 * ([AttemptLimiter.codesFrom]): after 5 free failures the blocks double from a minute up to an hour, about 30 guesses a
 * day per address, while one code lives [LIFETIME]. Even a million addresses at that pace would find one live invite
 * with odds of about 1 in 30,000.
 */
class Invites(
    private val storage: ServerStorage,
    private val passwords: PasswordHasher,
    private val registration: DeviceRegistration,
    private val clock: Clock,
    private val random: SecureRandom,
    private val attempts: AttemptLimiter,
) {
    /** A new code, to be shown once, or null when [actorId] is not an enabled admin. */
    fun issue(actorId: Long): String? {
        val code = OneTimeCode.generate(CODE_LENGTH, random)
        val expiresAt = clock.now().plus(LIFETIME).toEpochMilliseconds()
        return storage.transaction {
            storage.enabledAdmin(actorId) ?: return@transaction null
            storage.invites.insert(OneTimeCode.hash(code), expiresAt)
            code
        }
    }

    fun redeem(request: RedeemInviteRequest, remoteAddress: String): RedemptionOutcome {
        val keys = listOf(AttemptLimiter.codesFrom(remoteAddress))
        attempts.blockedFor(keys)?.let { return RedemptionOutcome.Throttled(it) }
        val codeHash = OneTimeCode.hash(request.code)
        if (!storage.invites.isUnexpired(codeHash, clock.now().toEpochMilliseconds())) {
            attempts.failed(keys)
            return RedemptionOutcome.Refused
        }
        // Checked after the code, so usernames can't be probed without an invite.
        val username = request.username.trim()
        if (!AccountCredentials.isValidUsername(username) || !AccountCredentials.isValidPassword(request.password)) {
            return RedemptionOutcome.InvalidCredentials
        }
        val passwordHash = passwords.hash(request.password)
        return storage.transaction {
            if (storage.accounts.byUsername(username) != null) return@transaction RedemptionOutcome.UsernameTaken
            val now = clock.now().toEpochMilliseconds()
            // Taken again under the write lock: the same code may have been redeemed since it was checked.
            if (!storage.invites.take(codeHash, now)) return@transaction RedemptionOutcome.Refused
            val account = storage.accounts.create(username, passwordHash, isAdmin = false, createdAtMillis = now)
            RedemptionOutcome.Registered(registration.link(account, request.device))
        }
    }

    fun forgetExpired() {
        storage.invites.deleteExpired(clock.now().toEpochMilliseconds())
    }

    companion object {
        val LIFETIME: Duration = 1.days
        private const val CODE_LENGTH = 8
    }
}

sealed interface RedemptionOutcome {
    data class Registered(val response: RegisterDeviceResponse) : RedemptionOutcome

    /** No unused invite has this code, or it expired. */
    data object Refused : RedemptionOutcome

    /** Too many wrong codes from this address; the code was not checked. */
    data class Throttled(val retryAfter: Duration) : RedemptionOutcome

    data object UsernameTaken : RedemptionOutcome

    /** The username or password breaks [AccountCredentials], which the app checks before sending. */
    data object InvalidCredentials : RedemptionOutcome
}
