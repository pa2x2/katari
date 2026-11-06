package app.katari.sync.client.record

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOne
import app.katari.sync.protocol.device.DeviceId
import app.katari.sync.protocol.record.RecordEnvelope
import dev.zacsweers.metro.Inject
import tachiyomi.data.DatabaseHandler

/** Local changes the drain stored and the server hasn't taken yet, as the envelopes a push sends. */
@Inject
class PendingChanges(private val handler: DatabaseHandler) {
    suspend fun next(limit: Long): List<RecordEnvelope> = handler.await {
        val device = sync_stateQueries.get().awaitAsOne().device_id?.let(::DeviceId) ?: return@await emptyList()
        sync_recordsQueries.pending(limit).awaitAsList().map { record ->
            RecordEnvelope(
                key = record.record_key,
                kind = record.kind,
                kindVersion = record.kind_version.toInt(),
                basedOn = record.based_on,
                changeId = checkNotNull(record.change_id),
                device = device,
                deleted = record.deleted,
                payload = record.payload,
            )
        }
    }

    suspend fun count(): Long = handler.awaitOne { sync_recordsQueries.pendingCount() }

    /**
     * The server took [changeId] of [key] as [sequence]. A change the drain stored since keeps waiting, now based on
     * [sequence].
     */
    suspend fun taken(key: String, changeId: String, sequence: Long) {
        handler.await { sync_recordsQueries.markPushed(sequence = sequence, changeId = changeId, recordKey = key) }
    }
}
