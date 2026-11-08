package app.katari.sync.server.device

import app.katari.sync.protocol.device.DeviceDescription
import app.katari.sync.protocol.device.DeviceId

data class Device(
    val id: DeviceId,
    val accountId: Long,
    val description: DeviceDescription,
    val registeredAtMillis: Long,
    val lastSeenMillis: Long,
    /** The position the device last pulled from, or null before its first pull. */
    val syncedUpTo: Long?,
    val isRevoked: Boolean,
)
