package app.katari.sync.client.record

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOne
import app.katari.sync.protocol.device.DeviceId
import app.katari.sync.protocol.record.RecordEnvelope
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import tachiyomi.data.DatabaseHandler

/** Local changes the drain stored and the server hasn't taken yet, as the envelopes a push sends. */
@Inject
class PendingChanges(private val handler: DatabaseHandler) {
    suspend fun next(limit: Long): List<RecordEnvelope> = after(afterKey = "", limit)

    /** The next [limit] changes in key order after [afterKey], so a push can go through all of them in chunks. */
    suspend fun after(afterKey: String, limit: Long): List<RecordEnvelope> = handler.await {
        val device = sync_stateQueries.get().awaitAsOne().device_id?.let(::DeviceId) ?: return@await emptyList()
        sync_recordsQueries.pendingAfter(afterKey, limit).awaitAsList().map { record ->
            RecordEnvelope(
                key = record.record_key,
                kind = record.kind,
                kindVersion = record.kind_version.toInt(),
                basedOn = record.based_on,
                changeId = checkNotNull(record.change_id),
                device = device,
                deleted = record.deleted,
                payload = record.payload,
                files = RecordFiles.of(record.kind, record.deleted, record.payload),
                group = RecordGroups.of(record.kind, record.record_key, record.deleted, record.payload),
            )
        }
    }

    suspend fun count(): Long = handler.awaitOne { sync_recordsQueries.pendingCount() }

    fun counts(): Flow<Long> = handler.subscribeToOne { sync_recordsQueries.pendingCount() }

    /**
     * The server took each of [changes] as its sequence. A change the drain stored since keeps waiting, now based on
     * that sequence. One transaction for them all: a first upload takes a whole library, and a commit for each record
     * held it up for minutes on a phone.
     */
    suspend fun taken(changes: List<Taken>) {
        if (changes.isEmpty()) return
        handler.await(inTransaction = true) {
            changes.forEach {
                sync_recordsQueries.markPushed(sequence = it.sequence, changeId = it.changeId, recordKey = it.key)
            }
        }
    }

    data class Taken(val key: String, val changeId: String, val sequence: Long)

    /**
     * The server holds no copy of [key] any more ([app.katari.sync.protocol.push.PushResult.sequence] 0): its last
     * one was a deletion every device had synced past. The change, merged with that deletion, is pushed as new.
     */
    suspend fun rebase(key: String) {
        handler.await { sync_recordsQueries.rebase(key) }
    }
}
