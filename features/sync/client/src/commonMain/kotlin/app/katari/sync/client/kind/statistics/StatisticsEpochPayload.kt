package app.katari.sync.client.kind.statistics

import app.katari.sync.client.payload.Stamped
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

/** When a profile's statistics start counting: the later of [startedAt] and [clearedAt]. */
@Serializable
data class StatisticsEpochPayload(
    @ProtoNumber(1) val profileUuid: String,
    /** Epoch milliseconds of the first activity any device recorded. */
    @ProtoNumber(2) val startedAt: Stamped<Long>,
    /** Epoch milliseconds of when history was last cleared; null if it never was. */
    @ProtoNumber(3) val clearedAt: Stamped<Long?> = Stamped(null, Stamped.UNKNOWN_EDIT),
)
