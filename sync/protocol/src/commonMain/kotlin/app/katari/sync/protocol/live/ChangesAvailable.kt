package app.katari.sync.protocol.live

import app.katari.sync.protocol.server.ServerIdentity
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

/**
 * Sent over the live channel when the account has changes up to [server]'s sequence. A device already at that
 * position ignores it, which also covers the announcement of its own push.
 */
@Serializable
data class ChangesAvailable(
    @ProtoNumber(1) val server: ServerIdentity,
)
