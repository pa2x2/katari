package app.katari.sync.protocol.push

import app.katari.sync.protocol.record.RecordEnvelope
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

/** Records changed on the device, already merged with everything it has pulled. Each is accepted or refused alone. */
@Serializable
data class PushRequest(
    @ProtoNumber(1) val records: List<RecordEnvelope> = emptyList(),
)
