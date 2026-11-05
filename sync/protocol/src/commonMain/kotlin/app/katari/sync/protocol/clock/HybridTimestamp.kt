package app.katari.sync.protocol.clock

import app.katari.sync.protocol.device.DeviceId
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoIntegerType
import kotlinx.serialization.protobuf.ProtoNumber
import kotlinx.serialization.protobuf.ProtoType

/**
 * When a field was last edited, by a [HybridLogicalClock]; the later timestamp wins when devices merge a field.
 * Timestamps are totally ordered, by [physicalMillis], then [counter], then [device], so two devices comparing the
 * same pair always pick the same winner. Payloads store one per synced field.
 */
@Serializable
data class HybridTimestamp(
    /** Epoch milliseconds, from the wall clock or from a later timestamp the device had already seen. */
    @ProtoNumber(1) val physicalMillis: Long,
    /** Orders timestamps that share [physicalMillis]. */
    @ProtoNumber(2) val counter: Int = 0,
    @ProtoNumber(3) @ProtoType(ProtoIntegerType.FIXED) val device: DeviceId,
) : Comparable<HybridTimestamp> {
    override fun compareTo(other: HybridTimestamp): Int = compareValuesBy(
        this,
        other,
        HybridTimestamp::physicalMillis,
        HybridTimestamp::counter,
        { it.device.value },
    )
}
