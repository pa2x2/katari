package app.katari.sync.client.trigger

import app.cash.sqldelight.Query
import app.cash.sqldelight.db.SqlDriver
import app.katari.sync.client.kind.SyncKinds
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate

/**
 * Writes to the tables capture watches, and to the outbox, where settings' edits go: each may be an edit to send.
 * The database tells its query listeners of every write the app's queries make; sync's own writes are heard too and
 * leave nothing to send, which the run finds out without asking the server.
 */
@Inject
class LocalChanges(private val driver: SqlDriver) {
    fun writes(): Flow<Unit> = callbackFlow {
        val listener = Query.Listener { trySend(Unit) }
        driver.addListener(*TABLES, listener = listener)
        awaitClose { driver.removeListener(*TABLES, listener = listener) }
    }.conflate()

    private companion object {
        val TABLES: Array<String> =
            (SyncKinds.all.flatMap { kind -> kind.capture.map { it.table } } + "sync_outbox").distinct().toTypedArray()
    }
}
