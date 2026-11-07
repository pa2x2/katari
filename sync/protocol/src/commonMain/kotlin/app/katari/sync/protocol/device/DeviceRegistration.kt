package app.katari.sync.protocol.device

import app.katari.sync.protocol.server.ServerIdentity
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoIntegerType
import kotlinx.serialization.protobuf.ProtoNumber
import kotlinx.serialization.protobuf.ProtoType

/**
 * Links a device to an account. Exactly one of [password] and [linkCode] is set: the account's own sign-in, or a
 * one-time code another device of the account showed ([LinkCode]).
 */
@Serializable
data class RegisterDeviceRequest(
    @ProtoNumber(1) val device: DeviceDescription,
    @ProtoNumber(2) val password: PasswordSignIn? = null,
    @ProtoNumber(3) val linkCode: String? = null,
)

@Serializable
data class PasswordSignIn(
    @ProtoNumber(1) val username: String,
    @ProtoNumber(2) val password: String,
)

/** The device's own credentials from here on; the password is never stored on the device. */
@Serializable
data class RegisterDeviceResponse(
    @ProtoNumber(1) @ProtoType(ProtoIntegerType.FIXED) val device: DeviceId,
    /** Long-lived and for this device only, so removing the device cancels nothing else. */
    @ProtoNumber(2) val token: String,
    @ProtoNumber(3) val accountName: String,
    @ProtoNumber(4) val server: ServerIdentity,
)
