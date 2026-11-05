package app.katari.sync.server.device

import app.katari.sync.protocol.error.SyncErrorCode
import app.katari.sync.server.storage.ServerStorage
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes

/** Finds the device a bearer token belongs to, and notes that it was seen. */
class DeviceAuthentication(
    private val storage: ServerStorage,
    private val clock: Clock,
) {
    fun authenticate(token: String?): AuthenticationOutcome {
        val device = token?.let { storage.devices.byTokenHash(DeviceTokens.hash(it)) }
            ?: return AuthenticationOutcome.Refused(SyncErrorCode.UNAUTHORIZED)
        if (device.isRevoked) return AuthenticationOutcome.Refused(SyncErrorCode.DEVICE_REVOKED)
        val account = storage.accounts.byId(device.accountId)
        if (account == null || account.isDisabled) return AuthenticationOutcome.Refused(SyncErrorCode.UNAUTHORIZED)
        val now = clock.now().toEpochMilliseconds()
        if (now - device.lastSeenMillis >= LAST_SEEN_RESOLUTION_MILLIS) storage.devices.touch(device.id, now)
        return AuthenticationOutcome.Authenticated(device)
    }

    private companion object {
        // Last seen only feeds the device list and the 180-day limit, so it needn't cost a write on every call.
        val LAST_SEEN_RESOLUTION_MILLIS = 1.minutes.inWholeMilliseconds
    }
}

sealed interface AuthenticationOutcome {
    data class Authenticated(val device: Device) : AuthenticationOutcome

    data class Refused(val code: SyncErrorCode) : AuthenticationOutcome
}
