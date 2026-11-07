package app.katari.sync.protocol.push

import app.katari.sync.protocol.record.RecordEnvelope
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

/**
 * Records changed on the device, already merged with everything it has pulled. Each is accepted or refused alone.
 *
 * [after] and [afterChain] are the position the device has pulled up to and the history chain there, empty when it
 * has none yet, as a [app.katari.sync.protocol.pull.PullRequest] gives them: when nothing but this push was written
 * since, the answer says so ([PushResponse.caughtUp]). A device that leaves [after] out is never told.
 */
@Serializable
data class PushRequest(
    @ProtoNumber(1) val records: List<RecordEnvelope> = emptyList(),
    @ProtoNumber(2) val after: Long? = null,
    @ProtoNumber(3) val afterChain: String = "",
)
