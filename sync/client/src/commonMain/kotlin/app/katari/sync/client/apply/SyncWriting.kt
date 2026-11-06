package app.katari.sync.client.apply

import app.cash.sqldelight.async.coroutines.awaitAsOne
import app.katari.sync.client.outbox.OutboxDrain
import app.katari.sync.client.setting.apply.SettingWrites
import app.katari.sync.protocol.device.DeviceId
import dev.zacsweers.metro.Inject
import mihon.feature.profiles.core.ProfileStore
import tachiyomi.data.Database
import tachiyomi.data.DatabaseHandler
import kotlin.time.Clock

/**
 * Runs sync's own writes to the device's rows: applying pulled records, and the changes sync makes on the person's
 * behalf (answers to questions, a first sync's choices, restore stamping). Each runs in one transaction that first
 * drains the edits made here so far, so they are merged rather than overwritten, and ends by deleting every outbox
 * row written inside it, so nothing sync wrote is captured as an edit. The transaction begins `IMMEDIATE`, holding
 * the database's only write lock from its first statement, so no other writer's edit can land among those rows.
 * Preferences reach the outbox later, from the stores' listeners; their records are stored before they are written,
 * so the drain finds nothing changed.
 */
@Inject
class SyncWriting(
    private val handler: DatabaseHandler,
    private val drain: OutboxDrain,
    private val settings: SettingWrites,
    private val profileStore: ProfileStore,
) {
    internal suspend fun <T> write(block: suspend Database.(ApplySession) -> T): T =
        handler.await(inTransaction = true) {
            do {
                val drained = drain.drain()
            } while (drained > 0)
            val device = checkNotNull(sync_stateQueries.get().awaitAsOne().device_id) { "Not registered with a server" }
            val capturedBefore = sync_outboxQueries.lastSeq().awaitAsOne()
            val session = ApplySession(this, settings, profileStore.currentProfileId, DeviceId(device)) {
                Clock.System.now().toEpochMilliseconds()
            }
            val result = block(session)
            session.newestSeen?.let { seen ->
                val clock = sync_clockQueries.get().awaitAsOne()
                val isNewer = seen.physicalMillis > clock.seen_physical ||
                    (seen.physicalMillis == clock.seen_physical && seen.counter > clock.seen_counter)
                if (isNewer) sync_clockQueries.setSeen(seen.physicalMillis, seen.counter.toLong())
            }
            sync_outboxQueries.deleteAfter(capturedBefore)
            result
        }
}
