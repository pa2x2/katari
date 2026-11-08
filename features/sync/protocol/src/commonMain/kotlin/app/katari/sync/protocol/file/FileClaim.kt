package app.katari.sync.protocol.file

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

/**
 * Sent before pushing records that list [files] ([app.katari.sync.protocol.record.RecordEnvelope.files]). The server
 * removes a file no record lists only once a day has passed since it was last claimed or uploaded, so claiming first
 * keeps a file the record is about to refer to from being removed before the push lands.
 */
@Serializable
data class FileClaimRequest(
    @ProtoNumber(1) val files: List<FileReference> = emptyList(),
)

/** The claimed files the server does not have; the device uploads them, which counts as claiming them. */
@Serializable
data class FileClaimResponse(
    @ProtoNumber(1) val missing: List<FileReference> = emptyList(),
)
