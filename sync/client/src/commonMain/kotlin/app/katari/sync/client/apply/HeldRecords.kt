package app.katari.sync.client.apply

import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import tachiyomi.data.DatabaseHandler

/**
 * Records pulled from the server that this app version can't read, by kind name: a newer app on another device syncs
 * them, and they wait here, applied once this app is updated. Changes made here to those records are not pushed
 * meanwhile, so screens can say what an update would bring along.
 */
@Inject
class HeldRecords(private val handler: DatabaseHandler) {
    fun unreadableKinds(): Flow<List<String>> = handler.subscribeToList { sync_pendingQueries.unreadableKinds() }
}
