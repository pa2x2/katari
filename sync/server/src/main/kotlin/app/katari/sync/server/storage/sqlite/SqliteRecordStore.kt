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
    private val chains = database.record_chainQueries

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

    override fun write(accountId: Long, record: RecordEnvelope, sequence: Long, chain: ByteArray) {
        database.transaction { writeInTransaction(accountId, record, sequence, chain) }
    }

    private fun writeInTransaction(accountId: Long, record: RecordEnvelope, sequence: Long, chain: ByteArray) {
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
            recordGroup = record.group,
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
        chains.insert(accountId = accountId, sequence = sequence, chain = chain)
    }

    override fun chainAt(accountId: Long, sequence: Long): ByteArray? =
        chains.chainAt(accountId, sequence).executeAsOneOrNull()

    override fun page(
        accountId: Long,
        after: Long,
        through: Long,
        limit: Int,
        includeDeleted: Boolean,
    ): List<SequencedRecord> = database.transactionWithResult {
        records.page(accountId, after, through, includeDeleted, limit.toLong(), ::StoredRow).executeAsList()
            .withFiles(accountId)
    }

    override fun group(accountId: Long, group: String, limit: Int): List<SequencedRecord> =
        database.transactionWithResult {
            records.inGroup(accountId, group, limit.toLong(), ::StoredRow).executeAsList().withFiles(accountId)
        }

    private fun List<StoredRow>.withFiles(accountId: Long): List<SequencedRecord> {
        val filesByKey = if (isEmpty()) {
            emptyMap()
        } else {
            files.forRecords(accountId, map { it.key }).executeAsList().groupBy(
                keySelector = { it.record_key },
                valueTransform = { FileReference(category = it.category, sha256 = it.sha256, size = it.size) },
            )
        }
        return map { row -> SequencedRecord(row.sequence, row.envelope(filesByKey[row.key].orEmpty())) }
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

    override fun clear(accountId: Long, sequence: Long, chain: ByteArray) = database.transaction {
        records.deleteAccount(accountId)
        changes.deleteAccount(accountId)
        chains.deleteAccount(accountId)
        accounts.restartHistory(sequence = sequence, id = accountId)
        chains.insert(accountId = accountId, sequence = sequence, chain = chain)
    }

    override fun forgetChainBefore(accountId: Long, before: Long) {
        chains.deleteUnusedBefore(accountId, before)
    }
}

/** One stored copy as a query reads it, before its files are added. */
private class StoredRow(
    val key: String,
    val sequence: Long,
    private val kind: String,
    private val kindVersion: Long,
    private val basedOn: Long,
    private val changeId: String,
    private val deviceId: Long,
    private val isDeleted: Boolean,
    private val payload: ByteArray,
    private val group: String,
) {
    fun envelope(files: List<FileReference>) = RecordEnvelope(
        key = key,
        kind = kind,
        kindVersion = kindVersion.toInt(),
        basedOn = basedOn,
        changeId = changeId,
        device = DeviceId(deviceId),
        deleted = isDeleted,
        payload = payload,
        files = files,
        group = group,
    )
}
