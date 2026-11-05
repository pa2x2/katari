package app.katari.sync.protocol.pull

import app.katari.sync.protocol.record.SequencedRecord
import app.katari.sync.protocol.server.ServerIdentity
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

/**
 * A page of records in sequence order. The device commits a page before asking for the next, so a pull interrupted
 * anywhere resumes from [nextCursor].
 */
@Serializable
data class PullPage(
    @ProtoNumber(1) val records: List<SequencedRecord> = emptyList(),
    /** The [PullRequest.after] of the next page, and the device's position once this page is applied. */
    @ProtoNumber(2) val nextCursor: Long,
    @ProtoNumber(3) val hasMore: Boolean = false,
    @ProtoNumber(4) val server: ServerIdentity,
)
