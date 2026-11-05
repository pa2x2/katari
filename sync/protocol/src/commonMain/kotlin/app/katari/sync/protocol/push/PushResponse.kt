package app.katari.sync.protocol.push

import app.katari.sync.protocol.server.ServerIdentity
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

/** One result per pushed record, in the order the records were sent. */
@Serializable
data class PushResponse(
    @ProtoNumber(1) val results: List<PushResult> = emptyList(),
    @ProtoNumber(2) val server: ServerIdentity,
)

@Serializable
data class PushResult(
    @ProtoNumber(1) val changeId: String,
    @ProtoNumber(2) val outcome: PushOutcome,
    /** The sequence the record was given when [PushOutcome.ACCEPTED], the server's current one when [PushOutcome.STALE]. */
    @ProtoNumber(3) val sequence: Long,
)

@Serializable
enum class PushOutcome {
    /** Stored; also the answer to a retried change the server already took, with the sequence it was given then. */
    @ProtoNumber(1)
    ACCEPTED,

    /**
     * Another device changed the record after the copy this change was based on. The device pulls, merges and pushes
     * the merged record again.
     */
    @ProtoNumber(2)
    STALE,
}
