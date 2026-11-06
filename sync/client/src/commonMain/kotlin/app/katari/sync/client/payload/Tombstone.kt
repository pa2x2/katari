package app.katari.sync.client.payload

import app.katari.sync.protocol.clock.HybridTimestamp
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

/** The payload of a deleted record ([app.katari.sync.protocol.record.RecordEnvelope.deleted]), whatever its kind. */
@Serializable
class Tombstone(
    @ProtoNumber(1) val deletedAt: HybridTimestamp,
    /**
     * The record whose row took this one's place, when the deletion merged two records into one: two categories of
     * the same name in a profile. Records that refer to this one apply to that one instead.
     */
    @ProtoNumber(2) val replacedBy: String? = null,
    /**
     * The record's payload as it was deleted, so a device that brings the record back, such as a profile kept after
     * the question about its deletion, recreates the row as it was. Empty when the deleting device held none.
     */
    @ProtoNumber(3) val last: ByteArray = ByteArray(0),
) {
    override fun equals(other: Any?): Boolean = other is Tombstone &&
        deletedAt == other.deletedAt &&
        replacedBy == other.replacedBy &&
        last.contentEquals(other.last)

    override fun hashCode(): Int = (deletedAt.hashCode() * 31 + replacedBy.hashCode()) * 31 + last.contentHashCode()
}
