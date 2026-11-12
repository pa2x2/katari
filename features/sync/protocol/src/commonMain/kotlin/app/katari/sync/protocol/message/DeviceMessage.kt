package app.katari.sync.protocol.message

import app.katari.sync.protocol.device.DeviceId
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoIntegerType
import kotlinx.serialization.protobuf.ProtoNumber
import kotlinx.serialization.protobuf.ProtoType
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days

/**
 * A message one device of an account sent to another, which the server holds until the target confirms it
 * ([FetchMessagesRequest.after]) or it expires. Messages carry what one device asks of another, never state: what must
 * still hold after [expiresAtMillis] belongs in a record. A device can be handed the same message more than once, so
 * handling it twice must do no harm.
 */
@Serializable
data class DeviceMessage(
    /** Grows with every message the server takes, so a device's messages are in the order they were sent. */
    @ProtoNumber(1) val number: Long,
    @ProtoNumber(2) @ProtoType(ProtoIntegerType.FIXED) val from: DeviceId,
    @ProtoNumber(3) val body: MessageBody,
    @ProtoNumber(4) val sentAtMillis: Long,
    @ProtoNumber(5) val expiresAtMillis: Long,
)

/** What a message says. The server never reads [payload], so it could be encrypted end to end. */
@Serializable
data class MessageBody(
    /** What [payload] holds; a device hands the message to what handles this kind. */
    @ProtoNumber(1) val kind: String,
    /** The format of [kind] the payload is written in. */
    @ProtoNumber(2) val kindVersion: Int,
    @ProtoNumber(3) val payload: ByteArray = ByteArray(0),
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return other is MessageBody &&
            kind == other.kind &&
            kindVersion == other.kindVersion &&
            payload.contentEquals(other.payload)
    }

    override fun hashCode(): Int {
        var result = kind.hashCode()
        result = 31 * result + kindVersion
        result = 31 * result + payload.contentHashCode()
        return result
    }
}

object MessageLimits {
    const val MAX_KIND_LENGTH: Int = 128
    const val MAX_PAYLOAD_BYTES: Int = 64 * 1024
    const val MAX_REPLACES_LENGTH: Int = 512
    val MAX_LIFETIME: Duration = 7.days
}
