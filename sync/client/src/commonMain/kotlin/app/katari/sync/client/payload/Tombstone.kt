package app.katari.sync.client.payload

import app.katari.sync.protocol.clock.HybridTimestamp
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

/** The payload of a deleted record ([app.katari.sync.protocol.record.RecordEnvelope.deleted]), whatever its kind. */
@Serializable
data class Tombstone(
    @ProtoNumber(1) val deletedAt: HybridTimestamp,
)
