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

/**
 * Records [values] (kind name and local ref) with no edit time, so each fills in what the account lacks and gives way
 * to every edit made anywhere. Called outside sync's own writes, as [recordEditsMadeNow] is.
 */
internal suspend fun DatabaseHandler.recordUnstamped(values: List<Pair<String, LocalRef>>) {
    if (values.isEmpty()) return
    await(inTransaction = true) {
        values.forEach { (kind, ref) -> sync_outboxQueries.recordUnstamped(kind = kind, localRef = ref.json) }
    }
}
