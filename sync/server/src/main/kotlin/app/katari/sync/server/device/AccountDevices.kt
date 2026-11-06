package app.katari.sync.server.device

import app.katari.sync.protocol.device.DeviceId
import app.katari.sync.protocol.device.LinkCode
import app.katari.sync.protocol.device.LinkedDevice
import app.katari.sync.protocol.device.LinkedDevices
import app.katari.sync.server.live.AccountChanges
import app.katari.sync.server.onetimecode.OneTimeCode
import app.katari.sync.server.storage.ServerStorage
import java.security.SecureRandom
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes

/** What a linked device can do with its account's devices: list them, remove one, and let a new one join. */
class AccountDevices(
    private val storage: ServerStorage,
    private val changes: AccountChanges,
    private val clock: Clock,
    private val random: SecureRandom,
) {
    fun list(accountId: Long): LinkedDevices = LinkedDevices(
        storage.devices.activeIn(accountId).map {
            LinkedDevice(
                id = it.id,
                description = it.description,
                lastSeenMillis = it.lastSeenMillis,
                syncedUpTo = it.syncedUpTo ?: 0,
            )
        },
    )

    /** False when the account has no such device that is not already revoked. */
    fun revoke(accountId: Long, id: DeviceId): Boolean {
        val revoked = storage.transaction {
            val device = storage.devices.byId(id)?.takeIf { it.accountId == accountId && !it.isRevoked }
            device?.also { storage.devices.revoke(it.id) } != null
        }
        if (revoked) {
            changes.announceRevoked(id)
            changes.announceDevicesChanged(accountId)
        }
        return revoked
    }

    /** A code that registers one device into the account without its password, for [LINK_CODE_LIFETIME]. */
    fun issueLinkCode(accountId: Long): LinkCode {
        val code = OneTimeCode.generate(LINK_CODE_LENGTH, random)
        val expiresAt = clock.now().plus(LINK_CODE_LIFETIME).toEpochMilliseconds()
        storage.linkCodes.insert(OneTimeCode.hash(code), accountId, expiresAt)
        return LinkCode(code = code, expiresAtMillis = expiresAt)
    }

    fun forgetExpiredLinkCodes() {
        storage.linkCodes.deleteExpired(clock.now().toEpochMilliseconds())
    }

    companion object {
        val LINK_CODE_LIFETIME = 2.minutes

        // 40 bits: guessing one in the two minutes it lives would take about 10^12 tries.
        private const val LINK_CODE_LENGTH = 8
    }
}
