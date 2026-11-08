package app.katari.sync.protocol.device

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

/** How a device presents itself in the account's device list; the server only stores and shows it. */
@Serializable
data class DeviceDescription(
    @ProtoNumber(1) val name: String,
    /** Free text such as "Android" or "Linux", so a new platform needs no server update. */
    @ProtoNumber(2) val platform: String,
    @ProtoNumber(3) val appVersion: String,
) {
    companion object {
        /** The longest any field may be; the server refuses a longer one, as it does a blank one. */
        const val MAX_FIELD_LENGTH = 128
    }
}
