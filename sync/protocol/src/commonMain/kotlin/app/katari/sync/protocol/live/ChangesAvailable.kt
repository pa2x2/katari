package app.katari.sync.protocol.live

import app.katari.sync.protocol.server.ServerIdentity
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

/**
 * Sent over the live channel when the account has changes up to [server]'s sequence, or its devices changed. A device
 * already at that position ignores the sequence, which also covers the announcement of its own push.
 */
@Serializable
data class ChangesAvailable(
    @ProtoNumber(1) val server: ServerIdentity,
    /**
     * Counts devices joining or leaving the account since the server started; a device lists the account's devices
     * again when it moves. 0 from a server that doesn't count them.
     */
    @ProtoNumber(2) val devices: Long = 0,
)
