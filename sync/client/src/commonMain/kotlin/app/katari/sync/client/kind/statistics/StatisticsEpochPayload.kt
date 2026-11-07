package app.katari.sync.client.kind.statistics

import app.katari.sync.client.payload.Stamped
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

/** When a profile's statistics start counting; clearing history moves it. */
@Serializable
data class StatisticsEpochPayload(
    @ProtoNumber(1) val profileUuid: String,
    /** Epoch milliseconds. */
    @ProtoNumber(2) val startedAt: Stamped<Long>,
)
