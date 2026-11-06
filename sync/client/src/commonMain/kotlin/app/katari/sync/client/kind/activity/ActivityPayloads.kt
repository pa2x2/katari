package app.katari.sync.client.kind.activity

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

/**
 * Activity is a log: each record is written by the one device it happened on and merges by union. A session grows
 * while it lasts, so a later copy of it holds at least as much as an earlier one.
 */
@Serializable
data class ActivitySessionPayload(
    /** A random UUID, already unique across devices. */
    @ProtoNumber(1) val sessionId: String,
    @ProtoNumber(2) val entryKey: String,
    /** Epoch milliseconds. */
    @ProtoNumber(3) val startedAt: Long,
    @ProtoNumber(4) val endedAt: Long,
    /** Milliseconds. */
    @ProtoNumber(5) val duration: Long,
)

/** The part of a session spent on one chapter on one local day. */
@Serializable
data class ActivitySegmentPayload(
    @ProtoNumber(1) val sessionId: String,
    @ProtoNumber(2) val chapterKey: String? = null,
    /** ISO date, in [timeZoneId]. */
    @ProtoNumber(3) val localDate: String,
    @ProtoNumber(4) val timeZoneId: String,
    @ProtoNumber(5) val startedAt: Long,
    @ProtoNumber(6) val endedAt: Long,
    @ProtoNumber(7) val duration: Long,
)

/** A chapter finished, by reading to its end or by marking it. */
@Serializable
data class ActivityCompletionPayload(
    @ProtoNumber(1) val entryKey: String,
    @ProtoNumber(2) val chapterKey: String? = null,
    @ProtoNumber(3) val sessionId: String? = null,
    /** Epoch milliseconds. */
    @ProtoNumber(4) val occurredAt: Long,
    @ProtoNumber(5) val localDate: String,
    @ProtoNumber(6) val timeZoneId: String,
    /** `consumption` or `manual`. */
    @ProtoNumber(7) val cause: String,
)
