package app.katari.sync.protocol.server

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

/**
 * Sent with every answer that moves a device's position, so the device notices when the server is not the one it
 * synced with before: a different [instanceId] means the server's storage was lost and started over, and a [sequence]
 * below the device's own position means it was restored from an old copy. In both cases the device's position means
 * nothing there and it resyncs in full instead of trusting it.
 */
@Serializable
data class ServerIdentity(
    /** Picked at random when the server's storage is created and kept for as long as that storage lives. */
    @ProtoNumber(1) val instanceId: String,
    /** The newest sequence number the server has given out in this account, 0 before the first write. */
    @ProtoNumber(2) val sequence: Long = 0,
)
