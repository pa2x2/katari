package app.katari.sync.client.apply

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOne
import app.katari.sync.client.connection.ThisDevice
import app.katari.sync.client.kind.profile.LocalProfileDeletion
import app.katari.sync.client.kind.replacement.carry.MigrationCarryRules
import app.katari.sync.client.outbox.OutboxDrain
import app.katari.sync.client.setting.apply.SettingWrites
import app.katari.sync.data.Sync_outbox
import app.katari.sync.protocol.device.DeviceId
import dev.zacsweers.metro.Inject
import logcat.LogPriority
import mihon.feature.profiles.core.ProfileStore
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
) {
    internal suspend fun <T> write(block: suspend Database.(ApplySession) -> T): T =
        write(takeAfter = null) { session, _ -> block(session) }

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
                return write(takeAfter = null, failing) { session, _ -> block(session) }
            } catch (e: RecordApplyFailure) {
                val before = failing[e.key]
                if (before == FailedStage.STORING || before == e.stage) throw e
                logcat(LogPriority.ERROR, e.cause) { "Holding aside sync record ${e.key}, which failed to apply" }
                failing[e.key] = e.stage
            }
        }
    }

    /**
     * Runs [block] as [write] does, handing it, instead of draining them, the outbox rows written after [takeAfter],
     * such as a restore's, so it can record them differently from a person's edits. Null takes none.
     */
    internal suspend fun <T> write(
        takeAfter: Long?,
        block: suspend Database.(ApplySession, taken: List<Sync_outbox>) -> T,
    ): T = write(takeAfter, failing = emptyMap(), block)

    private suspend fun <T> write(
        takeAfter: Long?,
        failing: Map<String, FailedStage>,
        block: suspend Database.(ApplySession, taken: List<Sync_outbox>) -> T,
    ): T = handler.await(inTransaction = true) {
        val taken = takeAfter?.let { seq ->
            sync_outboxQueries.after(seq).awaitAsList().also { sync_outboxQueries.deleteAfter(seq) }
        }.orEmpty()
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
            device = DeviceId(device),
            appVersion = thisDevice.appVersion,
            failing = failing,
        ) { Clock.System.now().toEpochMilliseconds() }
        val result = block(session, taken)
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
