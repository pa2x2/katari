package app.katari.sync.protocol.push

import app.katari.sync.protocol.record.SequencedRecord
import app.katari.sync.protocol.server.ServerIdentity
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

/** One result per pushed record, in the order the records were sent. */
@Serializable
data class PushResponse(
    @ProtoNumber(1) val results: List<PushResult> = emptyList(),
    @ProtoNumber(2) val server: ServerIdentity,
    /**
     * Nothing but this push was written since the request's [PushRequest.after], so the device holds everything up to
     * [server]'s sequence and moves its position there, with that chain, instead of pulling its own records back.
     * Always false from a server that predates it.
     */
    @ProtoNumber(3) val caughtUp: Boolean = false,
)

@Serializable
data class PushResult(
    @ProtoNumber(1) val changeId: String,
    @ProtoNumber(2) val outcome: PushOutcome,
    /**
     * The sequence the record was given when [PushOutcome.ACCEPTED], the server's current one when [PushOutcome.STALE].
     * A stale 0 means the server holds no copy of the record: its last copy was a deletion every device had synced
     * past, so the server dropped the marker. The device has applied that deletion, or its next pull ends in
     * [app.katari.sync.protocol.error.SyncErrorCode.CURSOR_EXPIRED] and a full resync finds it; it merges the change
     * with the deletion and pushes the result based on 0.
     */
    @ProtoNumber(3) val sequence: Long,
    /**
     * For a [PushOutcome.STALE] other than 0, the copy the server holds at [sequence], deletion marker or not. The
     * device merges it as it would a pulled one, as no pull may bring it: a marker before the device's position, or
     * one a pull leaving deletions out skipped. Always null from a server that predates it.
     */
    @ProtoNumber(4) val current: SequencedRecord? = null,
)

@Serializable
enum class PushOutcome {
    /** Stored; also the answer to a retried change the server already took, with the sequence it was given then. */
    @ProtoNumber(1)
    ACCEPTED,

    /**
     * Another device changed the record after the copy this change was based on. The device merges the server's copy
     * ([PushResult.current]), or pulls it from a server that doesn't hand it back, and pushes the merged record again.
     */
    @ProtoNumber(2)
    STALE,
}
