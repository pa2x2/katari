package app.katari.sync.protocol.account

import app.katari.sync.protocol.device.DeviceDescription
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

/**
 * Creates an account from a one-time invite an admin made on the server's web page, with the username and password
 * the invited person picks, and links the device sending it in the same step: the answer is a
 * [app.katari.sync.protocol.device.RegisterDeviceResponse]. The invite is used up only when the account is created,
 * so a taken username ([app.katari.sync.protocol.error.SyncErrorCode.USERNAME_TAKEN]) can be retried with another.
 */
@Serializable
data class RedeemInviteRequest(
    @ProtoNumber(1) val device: DeviceDescription,
    /** As the person typed it; case, dashes and spaces don't matter. */
    @ProtoNumber(2) val code: String,
    /** Must pass [AccountCredentials.isValidUsername]. */
    @ProtoNumber(3) val username: String,
    /** Must pass [AccountCredentials.isValidPassword]. */
    @ProtoNumber(4) val password: String,
)
