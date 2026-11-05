package app.katari.sync.server.storage.sqlite

import app.katari.sync.protocol.device.DeviceId
import app.katari.sync.protocol.file.FileReference
import app.katari.sync.protocol.record.RecordEnvelope
import app.katari.sync.protocol.record.SequencedRecord
import app.katari.sync.server.record.RecordStore
import app.katari.sync.server.record.RecordTotals

internal class SqliteRecordStore(private val database: SyncServerDatabase) : RecordStore {
    private val accounts = database.accountsQueries
    private val records = database.recordsQueries
    private val files = database.record_filesQueries
    private val changes = database.record_changesQueries

    override fun sequence(accountId: Long): Long = accounts.sequence(accountId).executeAsOne()

    override fun advanceSequence(accountId: Long): Long = database.transactionWithResult {
        accounts.advanceSequence(accountId)
        sequence(accountId)
    }

    override fun markersDroppedThrough(accountId: Long): Long =
        accounts.markersDroppedThrough(accountId).executeAsOne()

    override fun raiseMarkersDroppedThrough(accountId: Long, through: Long) {
        accounts.raiseMarkersDroppedThrough(through = through, id = accountId)
    }

    override fun sequenceOf(accountId: Long, key: String): Long? =
        records.sequenceOf(accountId, key).executeAsOneOrNull()

    override fun appliedChange(accountId: Long, changeId: String): Long? =
        changes.sequenceOf(accountId, changeId).executeAsOneOrNull()

    override fun write(accountId: Long, record: RecordEnvelope, sequence: Long) = database.transaction {
        records.upsert(
            accountId = accountId,
            recordKey = record.key,
            sequence = sequence,
            kind = record.kind,
            kindVersion = record.kindVersion.toLong(),
            basedOn = record.basedOn,
            changeId = record.changeId,
            deviceId = record.device.value,
            isDeleted = record.deleted,
            payload = record.payload,
        )
        files.deleteForRecord(accountId, record.key)
        if (!record.deleted) {
            record.files.forEach { file ->
                files.insert(
                    accountId = accountId,
                    recordKey = record.key,
                    category = file.category,
                    sha256 = file.sha256,
                    size = file.size,
                )
            }
        }
        changes.insert(accountId = accountId, changeId = record.changeId, sequence = sequence)
    }

    override fun page(
        accountId: Long,
        after: Long,
        through: Long,
        limit: Int,
        includeDeleted: Boolean,
    ): List<SequencedRecord> = database.transactionWithResult {
        val rows = records.page(accountId, after, through, includeDeleted, limit.toLong()).executeAsList()
        val filesByKey = if (rows.isEmpty()) {
            emptyMap()
        } else {
            files.forRecords(accountId, rows.map { it.record_key }).executeAsList().groupBy(
                keySelector = { it.record_key },
                valueTransform = { FileReference(category = it.category, sha256 = it.sha256, size = it.size) },
            )
        }
        rows.map { row ->
            SequencedRecord(
                sequence = row.sequence,
                record = RecordEnvelope(
                    key = row.record_key,
                    kind = row.kind,
                    kindVersion = row.kind_version.toInt(),
                    basedOn = row.based_on,
                    changeId = row.change_id,
                    device = DeviceId(row.device_id),
                    deleted = row.is_deleted,
                    payload = row.payload,
                    files = filesByKey[row.record_key].orEmpty(),
                ),
            )
        }
    }

    override fun liveTotals(accountId: Long): RecordTotals = records.liveTotals(accountId) { count, bytes ->
        RecordTotals(records = count, payloadBytes = bytes ?: 0)
    }.executeAsOne()

    override fun dropMarkersThrough(accountId: Long, through: Long): Long? = database.transactionWithResult {
        records.newestMarkerThrough(accountId, through).executeAsOne().sequence
            ?.also { records.deleteMarkersThrough(accountId, through) }
    }

    override fun forgetChangesThrough(accountId: Long, through: Long) {
        changes.deleteThrough(accountId, through)
    }
}
