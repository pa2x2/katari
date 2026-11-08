package app.katari.sync.protocol.record

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

/** A record as the server hands it out: the newest copy of its key, with the sequence that copy was given. */
@Serializable
data class SequencedRecord(
    /** What the device's next change to this record names as [RecordEnvelope.basedOn]. */
    @ProtoNumber(1) val sequence: Long,
    @ProtoNumber(2) val record: RecordEnvelope,
)
