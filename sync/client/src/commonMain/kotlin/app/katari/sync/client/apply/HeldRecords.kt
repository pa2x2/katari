package app.katari.sync.client.apply

import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import tachiyomi.data.DatabaseHandler

/**
 * Records pulled from the server that wait for this app to be updated: those it can't read, by kind name, which a
 * newer app on another device syncs, and those that failed to apply here (D067). Changes made here to either are not
 * pushed meanwhile, so screens can say what an update would bring along.
 */
@Inject
class HeldRecords(private val handler: DatabaseHandler) {
    fun unreadableKinds(): Flow<List<String>> = handler.subscribeToList { sync_pendingQueries.unreadableKinds() }

    fun failedCount(): Flow<Long> = handler.subscribeToOne { sync_pendingQueries.failedCount() }
}
