package app.katari.sync.protocol.record

import app.katari.sync.protocol.device.DeviceId
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoIntegerType
import kotlinx.serialization.protobuf.ProtoNumber
import kotlinx.serialization.protobuf.ProtoType

/**
 * One synced record as it travels and as the server stores it. Everything the server acts on is outside [payload]:
 * it keeps the newest envelope per [key] and accepts a push only when [basedOn] is the sequence it holds for that key,
 * which needs no look inside. Payloads could therefore be encrypted end to end without changing the protocol.
 */
@Serializable
data class RecordEnvelope(
    /** Opaque and unique within the account; the same record has the same key on every device. */
    @ProtoNumber(1) val key: String,
    /** What [payload] holds, such as an entry or a category. */
    @ProtoNumber(2) val kind: String,
    /** The format of [kind] the payload is written in; a device that cannot read it keeps the record until it can. */
    @ProtoNumber(3) val kindVersion: Int,
    /** The sequence of the server copy this change was made on, or 0 for a record the server has never had. */
    @ProtoNumber(4) val basedOn: Long = 0,
    /**
     * Picked by the device for this change and kept when the push is retried, so a retry after a lost answer is
     * recognised as the change the server already took rather than refused as stale.
     */
    @ProtoNumber(5) val changeId: String,
    @ProtoNumber(6) @ProtoType(ProtoIntegerType.FIXED) val device: DeviceId,
    /** A deletion, kept as a marker until every device has synced past it. */
    @ProtoNumber(7) val deleted: Boolean = false,
    /** Never read by the server. A deletion's payload may still carry the timestamps it is merged by. */
    @ProtoNumber(8) val payload: ByteArray = ByteArray(0),
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return other is RecordEnvelope &&
            key == other.key &&
            kind == other.kind &&
            kindVersion == other.kindVersion &&
            basedOn == other.basedOn &&
            changeId == other.changeId &&
            device == other.device &&
            deleted == other.deleted &&
            payload.contentEquals(other.payload)
    }

    override fun hashCode(): Int {
        var result = key.hashCode()
        result = 31 * result + kind.hashCode()
        result = 31 * result + kindVersion
        result = 31 * result + basedOn.hashCode()
        result = 31 * result + changeId.hashCode()
        result = 31 * result + device.hashCode()
        result = 31 * result + deleted.hashCode()
        result = 31 * result + payload.contentHashCode()
        return result
    }
}
