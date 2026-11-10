package app.katari.sync.client.kind.extension

import app.katari.sync.protocol.device.DeviceId
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoIntegerType
import kotlinx.serialization.protobuf.ProtoNumber
import kotlinx.serialization.protobuf.ProtoType

/** Whether each of the account's devices has the extension [pkgName] installed, as it last changed there. */
@Serializable
data class InstalledExtensionPayload(
    @ProtoNumber(1) val pkgName: String,
    /** One per device that ever had it, in device order. */
    @ProtoNumber(2) val devices: List<DeviceInstall> = emptyList(),
)

/** One device's install or uninstall of an extension. */
@Serializable
data class DeviceInstall(
    @ProtoNumber(1) @ProtoType(ProtoIntegerType.FIXED) val device: DeviceId,
    @ProtoNumber(2) val installed: Boolean,
    /** Epoch milliseconds; 0 for an extension the device had before it kept its installs, at an unknown time. */
    @ProtoNumber(3) val changedAt: Long,
) {
    companion object {
        /** Later changes after earlier ones; of two at once, the one on the larger device id, then an install. */
        val ORDER: Comparator<DeviceInstall> =
            compareBy<DeviceInstall>({ it.changedAt }, { it.device.value }, { it.installed })
    }
}
