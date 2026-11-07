package app.katari.sync.client.payload

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

/** A source as every device knows it, never by the local key a device stores it under. */
@Serializable
data class SourceReference(
    @ProtoNumber(1) val idScheme: String,
    @ProtoNumber(2) val externalId: Long,
)
