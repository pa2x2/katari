package app.katari.sync.client.firstsync

import app.cash.sqldelight.async.coroutines.await
import app.cash.sqldelight.db.SqlDriver
import app.katari.sync.client.capture.CaptureTrigger
import app.katari.sync.client.kind.SyncKinds
import app.katari.sync.client.kind.replacement.EntryReplacementKind
import app.katari.sync.client.outbox.OutboxDrain
import dev.zacsweers.metro.Inject

/**
 * Records the rows a device held before it joined an account (step 3 of [FirstSync]): every row a kind's insert
 * trigger would have captured, had sync been on when it was written, goes into the outbox the way the trigger writes
 * it, but touching no field ([app.katari.sync.client.record.FieldStamps]). The drain then stores each as a record
 * whose fields carry no edit time ([app.katari.sync.client.payload.Stamped.UNKNOWN_EDIT]): it fills in what the
 * account lacks and gives way to every edit made anywhere, which is how values from before sync merge.
 *
 * Entry replacements are left out. They are events of a migration made here before sync, whose old entry is gone
 * from this device; replayed on another device they would migrate entries the person kept there.
 *
 * Recording twice is harmless, as the drain finds the second time unchanged, so an interrupted step runs again whole.
 */
@Inject
class ExistingRows(private val driver: SqlDriver, private val drain: OutboxDrain) {
    suspend fun record() {
        statements.forEach { driver.execute(null, it, 0).await() }
        do {
            val drained = drain.drain()
        } while (drained > 0)
    }

    private companion object {
        val statements: List<String> = SyncKinds.all
            .filter { it != EntryReplacementKind }
            .flatMap { kind ->
                kind.capture.filter { it.event == CaptureTrigger.Event.INSERT }.map { insertStatement(kind.name, it) }
            }

        // The trigger's own expressions over NEW, with the table read under that name.
        fun insertStatement(kind: String, trigger: CaptureTrigger): String {
            val condition = trigger.condition?.let { " AND ($it)" }.orEmpty()
            return """
                INSERT INTO sync_outbox(kind, local_ref, fields, record_key, changed_at, seen_physical, seen_counter)
                SELECT '$kind', json_array(${trigger.ref.joinToString()}), 0, NULL, 0, C.seen_physical, C.seen_counter
                FROM main.${trigger.table} AS NEW, sync_clock AS C
                WHERE C.id = 0$condition
            """.trimIndent()
        }
    }
}
