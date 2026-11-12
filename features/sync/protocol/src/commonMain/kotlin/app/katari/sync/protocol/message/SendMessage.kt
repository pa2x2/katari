package app.katari.sync.protocol.message

import app.katari.sync.protocol.device.DeviceId
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoIntegerType
import kotlinx.serialization.protobuf.ProtoNumber
import kotlinx.serialization.protobuf.ProtoType

@Serializable
data class SendMessageRequest(
    /** Null sends the message to every other device of the account. */
    @ProtoNumber(1) @ProtoType(ProtoIntegerType.FIXED) val to: DeviceId? = null,
    @ProtoNumber(2) val body: MessageBody,
    /**
     * How long the server holds the message for a device that doesn't fetch it, at most [MessageLimits.MAX_LIFETIME].
     */
    @ProtoNumber(3) val lifetimeMillis: Long,
    /**
     * A message from this device with the same [replaces] still waiting for a target is dropped for this one, as for
     * something that changed again before it was delivered; empty replaces nothing. Opaque to the server.
     */
    @ProtoNumber(4) val replaces: String = "",
)

/** The devices the message was queued for; none when the account has no other device. */
@Serializable
data class SendMessageResponse(
    @ProtoNumber(1) val recipients: List<MessageRecipient> = emptyList(),
)

@Serializable
data class MessageRecipient(
    @ProtoNumber(1) @ProtoType(ProtoIntegerType.FIXED) val device: DeviceId,
    /** Whether the device's live channel is open, so it is told of the message at once. */
    @ProtoNumber(2) val connected: Boolean = false,
)
