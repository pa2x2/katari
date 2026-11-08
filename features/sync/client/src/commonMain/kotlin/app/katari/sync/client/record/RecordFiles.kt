package app.katari.sync.client.record

import app.katari.sync.client.kind.SyncKinds
import app.katari.sync.protocol.file.FileReference

/** The files a record lists in its envelope ([app.katari.sync.protocol.record.RecordEnvelope.files]). */
internal object RecordFiles {
    /** The files the record of [kind] names as [payload] holds it; none for a deletion, which keeps no file alive. */
    fun of(kind: String, deleted: Boolean, payload: ByteArray): List<FileReference> {
        if (deleted) return emptyList()
        val synced = SyncKinds.named(kind) ?: return emptyList()
        return filesOf(synced, payload)
    }

    private fun <P : Any> filesOf(kind: SyncedKind<P>, payload: ByteArray): List<FileReference> =
        kind.files(kind.decode(payload))
}
