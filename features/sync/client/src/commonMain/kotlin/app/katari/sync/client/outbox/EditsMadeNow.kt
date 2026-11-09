package app.katari.sync.client.outbox

import app.katari.sync.client.record.LocalRef
import tachiyomi.data.DatabaseHandler
import kotlin.time.Clock

/**
 * Records [edits] (kind name and local ref) as edits made now, as the settings capture records a person's, so they win
 * over every copy made before. Called outside sync's own writes, which drop what is captured inside them.
 */
internal suspend fun DatabaseHandler.recordEditsMadeNow(edits: List<Pair<String, LocalRef>>) {
    if (edits.isEmpty()) return
    val now = Clock.System.now().toEpochMilliseconds()
    await(inTransaction = true) {
        edits.forEach { (kind, ref) ->
            sync_outboxQueries.recordEdit(kind = kind, localRef = ref.json, changedAt = now)
        }
    }
}
