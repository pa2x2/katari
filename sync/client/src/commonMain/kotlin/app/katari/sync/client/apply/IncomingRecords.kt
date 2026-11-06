package app.katari.sync.client.apply

import app.cash.sqldelight.async.coroutines.awaitAsOne
import app.katari.sync.client.kind.SyncKinds
import app.katari.sync.client.outbox.OutboxDrain
import app.katari.sync.client.setting.apply.SettingWrites
import app.katari.sync.protocol.device.DeviceId
import app.katari.sync.protocol.pull.PullPage
import dev.zacsweers.metro.Inject
import mihon.feature.profiles.core.ProfileStore
import tachiyomi.data.Database
import tachiyomi.data.DatabaseHandler
import kotlin.time.Clock

/**
 * Where records pulled from the server enter the device. Transport calls [apply] with each page in the order the
 * server hands them out, and [retryHeld] at the start of each sync.
 *
 * Contract:
 * - A page is applied in one transaction together with the device's new position, so a sync interrupted anywhere
 *   resumes from the last page applied and never applies a page twice or half of one.
 * - Applying is idempotent: a page applied again, or records arriving in another order or batching, leave the same
 *   data, since merging is.
 * - Records within a page are applied parents first (profiles, entries, chapters, …, in [SyncKinds.all] order); a
 *   record whose parent comes in a later page waits for it.
 * - Edits on this device not drained into records yet are drained first, so they are merged rather than overwritten.
 * - Nothing applied is captured as a local edit: the capture triggers do fire for sync's own writes, and every outbox
 *   row written inside the transaction is deleted before it commits. The transaction begins `IMMEDIATE`, holding the
 *   database's only write lock from its first statement, so no other writer's edit can land among those rows.
 *   Preferences reach the outbox later, from the stores' listeners; their records are stored before they are written,
 *   so the drain finds nothing changed.
 * - What is left to push afterwards is in [app.katari.sync.client.record.PendingChanges]: records this device has
 *   edits of that the server's copy lacks, never records only applied here.
 */
@Inject
class IncomingRecords(
    private val handler: DatabaseHandler,
    private val drain: OutboxDrain,
    private val settings: SettingWrites,
    private val profileStore: ProfileStore,
) {
    suspend fun apply(page: PullPage) = applying { session ->
        page.records
            .sortedBy { kindOrder[it.record.kind] ?: Int.MAX_VALUE }
            .forEach { session.receive(it) }
        sync_stateQueries.setPosition(
            serverInstanceId = page.server.instanceId,
            cursor = page.nextCursor,
            chain = page.nextChain,
        )
    }

    /** Applies what was held for a reason that may have passed: an app update, the clock, a reference, a profile. */
    suspend fun retryHeld() = applying { session -> session.retryHeld() }

    private suspend fun applying(block: suspend Database.(ApplySession) -> Unit) {
        handler.await(inTransaction = true) {
            do {
                val drained = drain.drain()
            } while (drained > 0)
            val device = checkNotNull(sync_stateQueries.get().awaitAsOne().device_id) { "Not registered with a server" }
            val capturedBefore = sync_outboxQueries.lastSeq().awaitAsOne()
            val session = ApplySession(this, settings, profileStore.currentProfileId, DeviceId(device)) {
                Clock.System.now().toEpochMilliseconds()
            }
            block(session)
            session.newestSeen?.let { seen ->
                val clock = sync_clockQueries.get().awaitAsOne()
                val isNewer = seen.physicalMillis > clock.seen_physical ||
                    (seen.physicalMillis == clock.seen_physical && seen.counter > clock.seen_counter)
                if (isNewer) sync_clockQueries.setSeen(seen.physicalMillis, seen.counter.toLong())
            }
            sync_outboxQueries.deleteAfter(capturedBefore)
        }
    }

    private companion object {
        val kindOrder = SyncKinds.all.withIndex().associate { (index, kind) -> kind.name to index }
    }
}
