package app.katari.sync.server.record

import app.katari.sync.protocol.snapshot.SnapshotInfo
import app.katari.sync.server.storage.ServerStorage

/** How much a device taking the whole account will download. */
class SnapshotSummary(private val storage: ServerStorage) {
    fun of(accountId: Long): SnapshotInfo = storage.transaction {
        val records = storage.records.liveTotals(accountId)
        val files = storage.files.referencedTotals(accountId)
        SnapshotInfo(
            server = storage.identityOf(accountId),
            recordCount = records.records,
            recordBytes = records.payloadBytes,
            fileCount = files.files,
            fileBytes = files.bytes,
        )
    }
}
