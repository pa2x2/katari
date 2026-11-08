package app.katari.sync.protocol.device

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoIntegerType
import kotlinx.serialization.protobuf.ProtoNumber
import kotlinx.serialization.protobuf.ProtoType

@Serializable
data class LinkedDevices(
    @ProtoNumber(1) val devices: List<LinkedDevice> = emptyList(),
)

@Serializable
data class LinkedDevice(
    @ProtoNumber(1) @ProtoType(ProtoIntegerType.FIXED) val id: DeviceId,
    @ProtoNumber(2) val description: DeviceDescription,
    /** Epoch milliseconds of the device's last call, or 0 if it has made none since registering. */
    @ProtoNumber(3) val lastSeenMillis: Long = 0,
    /** The sequence the device has pulled up to; deletion markers are kept until every device has passed them. */
    @ProtoNumber(4) val syncedUpTo: Long = 0,
)

/**
 * Removes a device from the account, this one included when signing out. Its token stops working and it no longer
 * holds back the removal of old deletion markers.
 */
@Serializable
data class RevokeDeviceRequest(
    @ProtoNumber(1) @ProtoType(ProtoIntegerType.FIXED) val device: DeviceId,
)
