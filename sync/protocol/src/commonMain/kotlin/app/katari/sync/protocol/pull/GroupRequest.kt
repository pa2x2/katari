package app.katari.sync.protocol.pull

import app.katari.sync.protocol.record.SequencedRecord
import app.katari.sync.protocol.server.ServerIdentity
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

/**
 * Asks for the newest copy of every record in [group] ([app.katari.sync.protocol.record.RecordEnvelope.group]),
 * whatever the device's position, so what the person is about to look at arrives before the rest of a pull. It moves
 * no position and holds back no deletion marker: the device applies the records out of order, which merging allows,
 * and still pulls them in order later.
 */
@Serializable
data class GroupRequest(
    @ProtoNumber(1) val group: String,
)

/** The group's records in sequence order, deletions included. */
@Serializable
data class GroupRecords(
    @ProtoNumber(1) val records: List<SequencedRecord> = emptyList(),
    @ProtoNumber(2) val server: ServerIdentity,
)
