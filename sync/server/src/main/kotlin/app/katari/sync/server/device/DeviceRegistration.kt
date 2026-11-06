package app.katari.sync.server.device

import app.katari.sync.protocol.device.DeviceDescription
import app.katari.sync.protocol.device.DeviceId
import app.katari.sync.protocol.device.RegisterDeviceRequest
import app.katari.sync.protocol.device.RegisterDeviceResponse
import app.katari.sync.server.account.Account
import app.katari.sync.server.account.PasswordCheck
import app.katari.sync.server.account.PasswordCheckOutcome
import app.katari.sync.server.attemptlimit.AttemptLimiter
import app.katari.sync.server.onetimecode.OneTimeCode
import app.katari.sync.server.record.identityOf
import app.katari.sync.server.storage.ServerStorage
import app.katari.sync.server.token.BearerTokens
import java.security.SecureRandom
import kotlin.time.Clock
import kotlin.time.Duration

/**
 * Links a device to an account, by the account's password or by a link code another of its devices asked for, and
 * gives it an id and a token of its own. Device ids are random over 64 bits (never 0) and never given out twice by this
 * server, since revoked devices keep their rows and deleted accounts' devices are retired.
 *
 * Failed sign-ins are limited as [PasswordCheck] limits them; a link code names no account until it is right, so its
 * guesses are limited per address only ([AttemptLimiter.codesFrom]).
 */
class DeviceRegistration(
    private val storage: ServerStorage,
    private val passwordCheck: PasswordCheck,
    private val clock: Clock,
    private val random: SecureRandom,
    private val attempts: AttemptLimiter,
) {
    fun register(request: RegisterDeviceRequest, remoteAddress: String): RegistrationOutcome {
        val signIn = request.password
        if (signIn != null) {
            return when (val check = passwordCheck.check(signIn.username, signIn.password, remoteAddress)) {
                is PasswordCheckOutcome.Matched -> storage.transaction { linkEnabled(check.account, request.device) }
                PasswordCheckOutcome.Wrong -> RegistrationOutcome.Refused
                is PasswordCheckOutcome.Throttled -> RegistrationOutcome.Throttled(check.retryAfter)
            }
        }
        val keys = listOf(AttemptLimiter.codesFrom(remoteAddress))
        attempts.blockedFor(keys)?.let { return RegistrationOutcome.Throttled(it) }
        val code = checkNotNull(request.linkCode) { "A request has a password or a link code" }
        val outcome = storage.transaction {
            val account = storage.linkCodes.take(OneTimeCode.hash(code), clock.now().toEpochMilliseconds())
                ?.let(storage.accounts::byId)
            account?.let { linkEnabled(it, request.device) }
        }
        if (outcome == null) attempts.failed(keys)
        return outcome ?: RegistrationOutcome.Refused
    }

    /** Links a new device to [account]. Must run inside a transaction of [storage]. */
    fun link(account: Account, description: DeviceDescription): RegisterDeviceResponse {
        val id = newDeviceId()
        val token = BearerTokens.generate(random)
        storage.devices.insert(
            id = id,
            accountId = account.id,
            description = description.trimmed(),
            tokenHash = BearerTokens.hash(token),
            registeredAtMillis = clock.now().toEpochMilliseconds(),
        )
        return RegisterDeviceResponse(
            device = id,
            token = token,
            accountName = account.username,
            server = storage.identityOf(account.id),
        )
    }

    // Only a right password or link code gets this far, so saying the account is disabled gives nothing away.
    private fun linkEnabled(account: Account, description: DeviceDescription): RegistrationOutcome =
        if (account.isDisabled) {
            RegistrationOutcome.Disabled
        } else {
            RegistrationOutcome.Registered(link(account, description))
        }

    private fun newDeviceId(): DeviceId = generateSequence { DeviceId(random.nextLong()) }
        .first { it.value != 0L && !storage.devices.exists(it) }

    private fun DeviceDescription.trimmed() =
        DeviceDescription(name = name.trim(), platform = platform.trim(), appVersion = appVersion.trim())
}

sealed interface RegistrationOutcome {
    data class Registered(val response: RegisterDeviceResponse) : RegistrationOutcome

    data object Refused : RegistrationOutcome

    /** The sign-in was right, but the account is disabled. */
    data object Disabled : RegistrationOutcome

    /** Too many failed attempts; the sign-in was not checked. */
    data class Throttled(val retryAfter: Duration) : RegistrationOutcome
}
