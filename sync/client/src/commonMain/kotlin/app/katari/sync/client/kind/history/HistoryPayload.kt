package app.katari.sync.client.kind.history

import app.katari.sync.client.payload.Stamped
import app.katari.sync.protocol.clock.HybridTimestamp
import app.katari.sync.protocol.device.DeviceId
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoIntegerType
import kotlinx.serialization.protobuf.ProtoNumber
import kotlinx.serialization.protobuf.ProtoType

/**
 * When a chapter was last read and for how long. Time read is a running total, so each device keeps its own share
 * and a device's total is the sum of every share: time read on two devices adds up instead of one overwriting the
 * other. Time read on a chapter of an entry that moved to another profile before the device heard of it is carried to
 * the chapter's new record as a share of its own, so it adds to the time read there.
 */
@Serializable
data class HistoryPayload(
    @ProtoNumber(1) val entryKey: String,
    @ProtoNumber(2) val chapterKey: String,
    /** Epoch milliseconds, or null before the chapter was opened. */
    @ProtoNumber(3) val lastRead: Stamped<Long?>,
    @ProtoNumber(4) val timeRead: List<TimeReadShare> = emptyList(),
)

@Serializable
data class TimeReadShare(
    @ProtoNumber(1) @ProtoType(ProtoIntegerType.FIXED) val device: DeviceId,
    /** Milliseconds. */
    @ProtoNumber(2) val millis: Long,
    /** When [device] last changed its share; only that device ever does. */
    @ProtoNumber(3) val at: HybridTimestamp,
    /** The chapter record the time was read on, when it was carried over from another; null for this chapter's own. */
    @ProtoNumber(4) val from: String? = null,
)
