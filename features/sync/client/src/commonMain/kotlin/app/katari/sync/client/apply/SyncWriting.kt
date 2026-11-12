package app.katari.sync.client.apply

import app.cash.sqldelight.async.coroutines.awaitAsOne
import app.katari.profiles.core.ProfileStore
import app.katari.sync.client.connection.ThisDevice
import app.katari.sync.client.kind.profile.LocalProfileDeletion
import app.katari.sync.client.kind.replacement.carry.MigrationCarryRules
import app.katari.sync.client.outbox.OutboxDrain
import app.katari.sync.client.part.OffParts
import app.katari.sync.client.preference.SyncPreferences
import app.katari.sync.client.readelsewhere.ChaptersReadElsewhere
import app.katari.sync.client.readelsewhere.ChaptersReadElsewhereListener
import app.katari.sync.client.setting.apply.SettingWrites
import app.katari.sync.protocol.device.DeviceId
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CancellationException
import logcat.LogPriority
import tachiyomi.core.common.preference.plusAssign
import tachiyomi.core.common.util.system.logcat
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
    private val profileDeletion: LocalProfileDeletion,
    private val migrationRules: MigrationCarryRules,
    private val profileStore: ProfileStore,
    private val thisDevice: ThisDevice,
    private val preferences: SyncPreferences,
    private val readElsewhereListeners: () -> Set<ChaptersReadElsewhereListener>,
) {
    internal suspend fun <T> write(block: suspend Database.(ApplySession) -> T): T = write(failing = emptyMap(), block)

    /**
     * Runs [block] as [write] does, holding aside each record that fails to apply (D067). A failure rolls the whole
     * transaction back, so nothing of the records written before it is left half applied and the position stays where
     * it was; the transaction then runs again with that record held aside as far as it got, until one completes. Each
     * attempt holds one more record aside, or holds one held after storing it before storing it instead, so it ends.
     */
    internal suspend fun <T> writeHoldingFailures(block: suspend Database.(ApplySession) -> T): T {
        val failing = HashMap<String, FailedStage>()
        while (true) {
            try {
                return write(failing, block)
            } catch (e: RecordApplyFailure) {
                val before = failing[e.key]
                if (before == FailedStage.STORING || before == e.stage) throw e
                logcat(LogPriority.ERROR, e.cause) { "Holding aside sync record ${e.key}, which failed to apply" }
                failing[e.key] = e.stage
            }
        }
    }

    private suspend fun <T> write(
        failing: Map<String, FailedStage>,
        block: suspend Database.(ApplySession) -> T,
    ): T {
        var read = emptyList<ChaptersReadElsewhere>()
        val result = handler.await(inTransaction = true) {
            do {
                val drained = drain.drain()
            } while (drained > 0)
            val device = checkNotNull(sync_stateQueries.get().awaitAsOne().device_id) { "Not registered with a server" }
            val capturedBefore = sync_outboxQueries.lastSeq().awaitAsOne()
            val session = ApplySession(
                database = this,
                settings = settings,
                profileDeletion = profileDeletion,
                migrationRules = migrationRules,
                activeProfileId = profileStore.currentProfileId,
                parts = OffParts(preferences.choices(), this) { preferences.profilesAsking += it },
                device = DeviceId(device),
                appVersion = thisDevice.appVersion,
                failing = failing,
            ) { Clock.System.now().toEpochMilliseconds() }
            val written = block(session)
            session.finish()
            session.newestSeen?.let { seen ->
                val clock = sync_clockQueries.get().awaitAsOne()
                val isNewer = seen.physicalMillis > clock.seen_physical ||
                    (seen.physicalMillis == clock.seen_physical && seen.counter > clock.seen_counter)
                if (isNewer) sync_clockQueries.setSeen(seen.physicalMillis, seen.counter.toLong())
            }
            sync_outboxQueries.deleteAfter(capturedBefore)
            read = session.chaptersRead
            written
        }
        // Told only once committed, so listeners act on rows that stay
        if (read.isNotEmpty()) tellReadElsewhere(read)
        return result
    }

    private suspend fun tellReadElsewhere(reads: List<ChaptersReadElsewhere>) {
        readElsewhereListeners().forEach { listener ->
            try {
                listener.onReadElsewhere(reads)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logcat(LogPriority.ERROR, e) { "Acting on chapters read on other devices failed" }
            }
        }
    }
}
