package app.katari.sync.protocol.device

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

/** A one-time code a linked device shows so a new device can join the account without the password. */
@Serializable
data class LinkCode(
    @ProtoNumber(1) val code: String,
    /** Server time, in epoch milliseconds, after which the code no longer registers a device. */
    @ProtoNumber(2) val expiresAtMillis: Long,
)
