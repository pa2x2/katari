package app.katari.sync.protocol.live

import app.katari.sync.protocol.message.DeviceMessage
import app.katari.sync.protocol.server.ServerIdentity
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

/**
 * Sent over the live channel when the account has changes up to [server]'s sequence, its devices changed, or a message
 * came for the device. A device already at that position ignores the sequence, which also covers the announcement of
 * its own push.
 */
@Serializable
data class ChangesAvailable(
    @ProtoNumber(1) val server: ServerIdentity,
    /**
     * Counts devices joining or leaving the account since the server started; a device lists the account's devices
     * again when it moves. 0 from a server that doesn't count them.
     */
    @ProtoNumber(2) val devices: Long = 0,
    /** The newest message waiting for the device ([DeviceMessage.number]), or 0 when none is. */
    @ProtoNumber(3) val newestMessage: Long = 0,
    /**
     * Every message waiting for the device when together they are small enough, so it needn't fetch them; empty
     * otherwise, and the device fetches up to [newestMessage].
     */
    @ProtoNumber(4) val messages: List<DeviceMessage> = emptyList(),
)
