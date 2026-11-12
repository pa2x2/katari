package app.katari.sync.client.apply

import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.katari.sync.client.merge.RecordState
import app.katari.sync.data.Sync_records
import tachiyomi.data.Database

/**
 * What a record key stands for on this device. A record deleted in favour of another
 * ([app.katari.sync.client.payload.Tombstone.replacedBy]), as two categories merged or a chapter moved to another URL,
 * stands for that one.
 */
internal sealed interface Standing {
    /** The live [record] of [key], the one asked about or one that replaced it. */
    class Live(val key: String, val record: Sync_records) : Standing

    /** No record of [key] here yet, the one asked about or one that replaced it. */
    class Missing(val key: String) : Standing

    /** Deleted, in favour of no record. */
    data object Gone : Standing
}

internal suspend fun Database.standing(recordKey: String): Standing {
    var key = recordKey
    repeat(MAX_REPLACEMENTS) {
        val stored = sync_recordsQueries.getByKey(key).awaitAsOneOrNull() ?: return Standing.Missing(key)
        if (!stored.deleted) return Standing.Live(key, stored)
        key = (RecordState.of(true, stored.payload) as RecordState.Deleted).tombstone.replacedBy
            ?: return Standing.Gone
    }
    return Standing.Gone
}

/** Records replaced by ones replaced again; a chain this long can only be a loop. */
private const val MAX_REPLACEMENTS = 16
