package app.katari.sync.client

import app.cash.sqldelight.async.coroutines.await
import app.cash.sqldelight.db.QueryResult
import app.katari.sync.protocol.pull.PullPage
import app.katari.sync.protocol.record.SequencedRecord
import app.katari.sync.protocol.server.ServerIdentity

/**
 * Hands records between in-process devices without a server: what a device pushes gets the next sequence numbers and
 * reaches the other devices in whichever order and batching a test picks.
 */
internal class RecordExchange {
    private var sequence = 0L
    private val server = ServerIdentity(instanceId = "exchange")

    /** Everything [device] has to push, taken as the server takes it. */
    suspend fun pushFrom(device: SyncTestDevice): List<SequencedRecord> {
        device.settingsCapture.flush()
        device.drainAll()
        return device.pending.next(Long.MAX_VALUE).map { envelope ->
            SequencedRecord(++sequence, envelope).also {
                device.pending.taken(envelope.key, envelope.changeId, it.sequence)
            }
        }
    }

    suspend fun deliver(device: SyncTestDevice, records: List<SequencedRecord>) {
        device.incoming.apply(PullPage(records, nextCursor = sequence, server = server))
    }

    /**
     * Passes every device's pushes to the others until none has anything left to push, each batch reordered by
     * [order] and delivered one record per page when [onePerPage].
     */
    suspend fun settle(
        devices: List<SyncTestDevice>,
        onePerPage: Boolean = false,
        order: (List<SequencedRecord>) -> List<SequencedRecord> = { it },
    ) {
        repeat(MAX_ROUNDS) {
            var pushed = false
            devices.forEach { from ->
                val records = order(pushFrom(from))
                if (records.isEmpty()) return@forEach
                pushed = true
                devices.filter { it !== from }.forEach { to ->
                    if (onePerPage) records.forEach { deliver(to, listOf(it)) } else deliver(to, records)
                }
            }
            if (!pushed) return
        }
        error("Devices still pushing after $MAX_ROUNDS rounds")
    }

    private companion object {
        const val MAX_ROUNDS = 10
    }
}

/** The person's data on [this] device by what names it on every device, never by local IDs, one line per row. */
internal suspend fun SyncTestDevice.library(): List<String> = listOf(
    """
    SELECT json_array('profile', uuid, name, color_seed, position, is_archived) FROM profiles
    """,
    """
    SELECT json_array('entry', P.uuid, S.id_scheme, S.external_id, E.url, E.type, E.favorite, E.date_added,
        E.display_name, E.notes, E.library_pinned, E.viewer, E.chapter_flags)
    FROM entries E JOIN profiles P ON P._id = E.profile_id JOIN source_identities S ON S.local_key = E.source
    """,
    """
    SELECT json_array('chapter', E.url, C.url, C.read, C.bookmark)
    FROM chapters C JOIN entries E ON E._id = C.entry_id
    """,
    """
    SELECT json_array('history', C.url, H.last_read, H.time_read)
    FROM history H JOIN chapters C ON C._id = H.chapter_id
    """,
    """
    SELECT json_array('progress', E.url, R.content_key, R.resource_key, R.position, R.completed)
    FROM entry_progress_state R JOIN entries E ON E._id = R.entry_id
    """,
    """
    SELECT json_array('category', P.uuid, C.name, C.sort, C.flags)
    FROM categories C JOIN profiles P ON P._id = C.profile_id WHERE C._id != 0
    """,
    """
    SELECT json_array('in category', E.url, C.name)
    FROM entries_categories M JOIN entries E ON E._id = M.entry_id JOIN categories C ON C._id = M.category_id
    """,
    """
    SELECT json_array('session', A.session_id, E.url, A.started_at, A.ended_at, A.duration)
    FROM activity_sessions A JOIN entries E ON E._id = A.entry_id
    """,
    """
    SELECT json_array('tracker', E.url, T.sync_id, T.remote_id, T.status, T.score)
    FROM entry_sync T JOIN entries E ON E._id = T.entry_id
    """,
).flatMap { rows(it) }.sorted()

/** The single text column [sql] selects, row by row. */
internal suspend fun SyncTestDevice.rows(sql: String): List<String> = driver.executeQuery(
    identifier = null,
    sql = sql.trimIndent(),
    mapper = { cursor ->
        QueryResult.AsyncValue {
            buildList {
                while (cursor.next().await()) add(cursor.getString(0).orEmpty())
            }
        }
    },
    parameters = 0,
).await()
