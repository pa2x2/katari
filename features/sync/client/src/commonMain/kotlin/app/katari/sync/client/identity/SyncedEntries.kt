package app.katari.sync.client.identity

import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.katari.sync.client.kind.entry.EntryKind
import app.katari.sync.client.record.LocalRef
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import tachiyomi.data.DatabaseHandler

/**
 * Entries as the account's devices name them to each other: by the key of the entry's record, the same on every
 * device. Only an entry that syncs has one here, as one in the library or with something read of it does.
 */
@Inject
class SyncedEntries(private val handler: DatabaseHandler) {
    /** The key of [entryId]'s record; null when the entry doesn't sync, so no other device knows it. */
    suspend fun keyOf(entryId: Long): String? = handler.await {
        sync_recordsQueries.getByLocalRef(EntryKind.name, EntryKind.ref(entryId).json).awaitAsOneOrNull()
            ?.takeIf { !it.deleted }
            ?.record_key
    }

    /** The entries that sync, and each change to them, as a new one in the library joins once its record is stored. */
    fun syncing(): Flow<Set<Long>> = handler.subscribeToList { sync_recordsQueries.liveLocalRefs(EntryKind.name) }
        .map { refs -> refs.mapTo(mutableSetOf()) { LocalRef(it).long(0) } }
        .distinctUntilChanged()

    /** The entry here whose record has [key]; null while that record hasn't come, or its entry is gone. */
    suspend fun entryOf(key: String): Long? = handler.await {
        sync_recordsQueries.getByKey(key).awaitAsOneOrNull()
            ?.takeIf { it.kind == EntryKind.name && !it.deleted }
            ?.local_ref
            ?.let { LocalRef(it).long(0) }
    }
}
