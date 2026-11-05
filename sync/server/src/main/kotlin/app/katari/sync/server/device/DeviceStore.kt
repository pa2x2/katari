package app.katari.sync.server.device

import app.katari.sync.protocol.device.DeviceDescription
import app.katari.sync.protocol.device.DeviceId

interface DeviceStore {
    /** Revoked devices count too, since their ids are never given out again. */
    fun exists(id: DeviceId): Boolean

    fun insert(
        id: DeviceId,
        accountId: Long,
        description: DeviceDescription,
        tokenHash: ByteArray,
        registeredAtMillis: Long,
    )

    fun byTokenHash(tokenHash: ByteArray): Device?

    fun byId(id: DeviceId): Device?

    /** The account's devices that are not revoked, oldest first. */
    fun activeIn(accountId: Long): List<Device>

    /** Moves the device's last-seen time forward to [seenAtMillis]; it never moves back. */
    fun touch(id: DeviceId, seenAtMillis: Long)

    fun setSyncedUpTo(id: DeviceId, position: Long)

    fun revoke(id: DeviceId)

    /**
     * The lowest position among the account's devices that still hold back deletion markers: not revoked, pulled at
     * least once, and seen at [seenSinceMillis] or later. Null when no device does.
     */
    fun oldestHeldPosition(accountId: Long, seenSinceMillis: Long): Long?
}
