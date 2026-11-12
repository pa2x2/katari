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
    /**
     * The account's sequence when the device's latest download that leaves deletions out
     * ([app.katari.sync.protocol.pull.PullRequest.liveOnly]) began, or null before one, or for one begun before the
     * server kept this.
     */
    val downloadStart: Long?,
    val isRevoked: Boolean,
)
