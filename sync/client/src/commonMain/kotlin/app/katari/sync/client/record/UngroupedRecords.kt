package app.katari.sync.client.record

import app.cash.sqldelight.async.coroutines.awaitAsList
import dev.zacsweers.metro.Inject
import tachiyomi.data.Database
import tachiyomi.data.DatabaseHandler

/**
 * The records stored before `sync_records` kept their groups, which get the group each is in so that finding an
 * entry's records by group misses none of them. Records stored since carry theirs already, so once filled this finds
 * nothing to do.
 */
@Inject
class UngroupedRecords(private val handler: DatabaseHandler) {
    /** Fills them in a transaction per batch, so no write waits long behind it. */
    suspend fun fill() {
        do {
            val filled = handler.await(inTransaction = true) { fillRecordGroups(limit = BATCH) }
        } while (filled > 0)
    }
}

/** Fills the groups of up to [limit] records that have none, all of them by default; returns how many it filled. */
internal suspend fun Database.fillRecordGroups(limit: Long = Long.MAX_VALUE): Int {
    var filled = 0
    while (filled < limit) {
        val batch = sync_recordsQueries.ungrouped(minOf(BATCH, limit - filled)).awaitAsList()
        if (batch.isEmpty()) break
        batch.forEach { record ->
            sync_recordsQueries.setGroup(
                recordGroup = RecordGroups.of(record.kind, record.record_key, record.deleted, record.payload),
                recordKey = record.record_key,
            )
        }
        filled += batch.size
    }
    return filled
}

private const val BATCH = 500L
