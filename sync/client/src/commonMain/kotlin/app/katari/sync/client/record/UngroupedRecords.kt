package app.katari.sync.client.record

import app.cash.sqldelight.async.coroutines.awaitAsList
import tachiyomi.data.Database

/**
 * Gives the records stored before `sync_records` kept their groups the group each is in, so finding an entry's
 * records by group misses none of them. Records stored since carry theirs already, so after the first call this finds
 * nothing to do.
 */
internal suspend fun Database.fillRecordGroups() {
    while (true) {
        val batch = sync_recordsQueries.ungrouped(BATCH).awaitAsList()
        if (batch.isEmpty()) return
        batch.forEach { record ->
            sync_recordsQueries.setGroup(
                recordGroup = RecordGroups.of(record.kind, record.record_key, record.deleted, record.payload),
                recordKey = record.record_key,
            )
        }
    }
}

private const val BATCH = 500L
