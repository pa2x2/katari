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
    /**
     * The kinds of [app.katari.sync.protocol.message.DeviceMessage] this device acts on, so another device offers to
     * send it only what it can take. An app from before the field lists none.
     */
    @ProtoNumber(4) val messageKinds: List<String> = emptyList(),
) {
    companion object {
        /** The longest the name, platform or app version may be; the server refuses longer or blank ones. */
        const val MAX_FIELD_LENGTH = 128

        /** The most [messageKinds] a device may list. */
        const val MAX_MESSAGE_KINDS = 64
    }
}
