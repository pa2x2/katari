package app.katari.sync.server.device

import app.katari.sync.protocol.device.DeviceDescription
import app.katari.sync.protocol.device.DeviceId
import app.katari.sync.protocol.device.PasswordSignIn
import app.katari.sync.protocol.device.RegisterDeviceRequest
import app.katari.sync.protocol.device.RegisterDeviceResponse
import app.katari.sync.server.account.Account
import app.katari.sync.server.account.PasswordHasher
import app.katari.sync.server.attemptlimit.AttemptLimiter
import app.katari.sync.server.onetimecode.OneTimeCode
import app.katari.sync.server.record.identityOf
import app.katari.sync.server.storage.ServerStorage
import java.security.SecureRandom
import kotlin.time.Clock
import kotlin.time.Duration

/**
 * Links a device to an account, by the account's password or by a link code another of its devices asked for, and
 * gives it an id and a token of its own. Device ids are random over 64 bits (never 0) and never given out twice by this
 * server, since revoked devices keep their rows.
 *
 * Failed sign-ins are limited per remote address and, for passwords, per account; a link code names no account until
 * it is right, so its guesses are limited per address only.
 */
class DeviceRegistration(
    private val storage: ServerStorage,
    private val passwords: PasswordHasher,
    private val clock: Clock,
    private val random: SecureRandom,
    private val attempts: AttemptLimiter,
) {
    fun register(request: RegisterDeviceRequest, remoteAddress: String): RegistrationOutcome {
        val keys = buildList {
            add(AttemptLimiter.address(remoteAddress))
            request.password?.let { add(AttemptLimiter.account(it.username)) }
        }
        attempts.blockedFor(keys)?.let { return RegistrationOutcome.Throttled(it) }
        val response = registerChecked(request)
        if (response == null) {
            attempts.failed(keys)
            return RegistrationOutcome.Refused
        }
        attempts.succeeded(keys)
        return RegistrationOutcome.Registered(response)
    }

    /** Null when the sign-in is refused: wrong password, unknown, used or expired link code, or a disabled account. */
    private fun registerChecked(request: RegisterDeviceRequest): RegisterDeviceResponse? {
        val passwordAccount = request.password?.let { signIn -> passwordAccount(signIn) ?: return null }
        return storage.transaction {
            val account = passwordAccount
                ?: request.linkCode?.let(::linkCodeAccount)
                ?: return@transaction null
            if (account.isDisabled) return@transaction null
            val id = newDeviceId()
            val token = DeviceTokens.generate(random)
            storage.devices.insert(
                id = id,
                accountId = account.id,
                description = request.device.trimmed(),
                tokenHash = DeviceTokens.hash(token),
                registeredAtMillis = clock.now().toEpochMilliseconds(),
            )
            RegisterDeviceResponse(
                device = id,
                token = token,
                accountName = account.username,
                server = storage.identityOf(account.id),
            )
        }
    }

    // Hashing is slow, so it runs before the transaction rather than holding the database's write lock.
    private fun passwordAccount(signIn: PasswordSignIn): Account? {
        val account = storage.accounts.byUsername(signIn.username.trim())
        if (account == null) {
            passwords.verifyNothing(signIn.password)
            return null
        }
        return account.takeIf { passwords.verify(signIn.password, it.passwordHash) }
    }

    private fun linkCodeAccount(code: String): Account? = storage.linkCodes
        .take(OneTimeCode.hash(code), clock.now().toEpochMilliseconds())
        ?.let(storage.accounts::byId)

    private fun newDeviceId(): DeviceId = generateSequence { DeviceId(random.nextLong()) }
        .first { it.value != 0L && !storage.devices.exists(it) }

    private fun DeviceDescription.trimmed() =
        DeviceDescription(name = name.trim(), platform = platform.trim(), appVersion = appVersion.trim())
}

sealed interface RegistrationOutcome {
    data class Registered(val response: RegisterDeviceResponse) : RegistrationOutcome

    data object Refused : RegistrationOutcome

    /** Too many failed attempts; the sign-in was not checked. */
    data class Throttled(val retryAfter: Duration) : RegistrationOutcome
}
