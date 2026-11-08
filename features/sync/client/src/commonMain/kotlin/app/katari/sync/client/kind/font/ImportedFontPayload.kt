package app.katari.sync.client.kind.font

import app.katari.sync.client.payload.Stamped
import app.katari.sync.client.payload.SyncedFile
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

/**
 * A font a profile imported for the book reader, by the file name the reader's font choice names it with, so the
 * choice means the same font on every device.
 */
@Serializable
data class ImportedFontPayload(
    @ProtoNumber(1) val profileUuid: String,
    @ProtoNumber(2) val fileName: String,
    @ProtoNumber(3) val file: Stamped<SyncedFile>,
)
