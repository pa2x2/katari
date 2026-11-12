package app.katari.sync.client.restore

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOne
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.katari.sync.client.apply.ApplySession
import app.katari.sync.client.apply.SyncWriting
import app.katari.sync.client.kind.SyncKinds
import app.katari.sync.client.kind.setting.ProfileSettings
import app.katari.sync.client.kind.setting.SharedSettings
import app.katari.sync.client.kind.setting.SourceSettingKind
import app.katari.sync.client.merge.RecordState
import app.katari.sync.client.payload.Tombstone
import app.katari.sync.client.record.FieldStamps
import app.katari.sync.client.record.LocalRead
import app.katari.sync.client.record.LocalReads
import app.katari.sync.client.record.LocalRef
import app.katari.sync.client.record.SyncedKind
import app.katari.sync.client.run.SyncRuns
import app.katari.sync.client.setting.capture.SettingsCapture
import app.katari.sync.client.setting.read.SettingReads
import app.katari.sync.protocol.clock.HybridTimestamp
import app.katari.sync.protocol.device.DeviceId
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.data.Database
import tachiyomi.data.DatabaseHandler

/**
 * Restoring a backup on a synced device (D046, D046a).
 *
 * [restoreOnThisDevice] counts what the restore writes as changes made when the backup was made: each row and
 * preference it leaves is recorded with the backup's creation time and merged with this device's copy, which holds
 * what other devices changed since, and the row is written back from the result. So an edit made anywhere after the
 * backup survives the restore, and the backup fills in only what is older or missing.
 *
 * Rolling the whole account back needs no stamping of its own. The rollback empties the person's data on this device
 * and then restores the backup, both with capture on and stamped now like any edit: every record the backup doesn't
 * have is deleted after every edit it holds, and every record it does have is written again after its own deletion,
 * so its values win everywhere. Devices apply both as they apply any change, and an edit one of them makes after
 * hearing of the rollback wins over it again. [AccountRollback] does the emptying.
 */
@Inject
class RestoreStamping(
    private val handler: DatabaseHandler,
    private val writing: SyncWriting,
    private val capture: SettingsCapture,
    private val settings: SettingReads,
    private val runs: SyncRuns,
) {
    /**
     * Runs [restore], which restores a backup created at [backupCreatedAt] (epoch milliseconds), and records what it
     * wrote as changes made then, whether it completes, fails or is cancelled. Without sync it only runs [restore].
     *
     * No sync run goes on meanwhile: a pull would apply records over rows the restore wrote before they are merged.
     * The restore is kept in `sync_state` while it runs, and no drain takes the outbox rows written meanwhile, as each
     * would be recorded as an edit made when it was written; a process that stops before they are recorded records
     * them once it starts again ([finishInterrupted]). Edits the person makes while a restore runs, which goes on in
     * the background, are among them: nothing tells them apart from the restore's, so they are recorded as made when
     * the backup was, losing only to changes made elsewhere since.
     *
     * Preference edits are not captured while [restore] runs; every person-level preference is compared with this
     * device's copy afterwards instead, since a store's listeners may hear a restore's writes only after it returns
     * and would record them as edits made then.
     */
    suspend fun <T> restoreOnThisDevice(backupCreatedAt: Long, restore: suspend () -> T): T {
        val state = handler.await { sync_stateQueries.get().awaitAsOne() }
        if (!state.enabled || state.device_id == null) return restore()
        return runs.exclusive {
            // One the last process stopped in, if this runs first, as when the platform runs the restore again
            finishLeftOver()
            capture.flush()
            capture.stop()
            handler.await { sync_stateQueries.startRestore(backupCreatedAt) }
            try {
                restore()
            } finally {
                capture.start()
                // What a failed or cancelled restore wrote stays, so it is recorded as well
                withContext(NonCancellable) { stampRestored() }
            }
        }
    }

    /**
     * Records what a restore the last process stopped in had written, as [restoreOnThisDevice] would have. Runs at
     * process start, once capture has resumed, as preferences the restore wrote may have reached their stores only.
     */
    suspend fun finishInterrupted() {
        if (restoreLeftOver()) runs.exclusive { finishLeftOver() }
    }

    private suspend fun restoreLeftOver(): Boolean =
        handler.await { sync_stateQueries.get().awaitAsOne().restore_outbox_seq } != null

    private suspend fun finishLeftOver() {
        if (!restoreLeftOver()) return
        try {
            stampRestored()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Logged where it failed; its rows drain as edits made when they were written
        }
    }

    /**
     * Records the outbox rows written since the restore under way started, and every preference, as changed when its
     * backup was made, and ends the restore. One that fails to be recorded ends too, logged, so its rows don't keep
     * every edit after them from syncing; they drain as edits made when they were written.
     */
    private suspend fun stampRestored() {
        try {
            writing.write { session ->
                val state = sync_stateQueries.get().awaitAsOne()
                val before = state.restore_outbox_seq ?: return@write
                val device = DeviceId(checkNotNull(state.device_id))
                val at = HybridTimestamp(checkNotNull(state.restore_backup_created_at), 0, device)
                val reads = LocalReads(this, device, settings)
                val taken = sync_outboxQueries.after(before).awaitAsList()
                sync_outboxQueries.deleteAfter(before)
                sync_stateQueries.endRestore()
                val touched = taken.groupBy { it.kind to it.local_ref }
                    .mapNotNull { (located, rows) ->
                        val kind = SyncKinds.named(located.first) ?: return@mapNotNull null
                        Touched(kind, LocalRef(located.second), rows.firstNotNullOfOrNull { it.record_key })
                    }
                (touched + preferences(this))
                    .sortedBy { kindOrder.getValue(it.kind.name) }
                    .forEach { stamp(session, reads, it.kind, it, at) }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e) { "Couldn't record a restore as made when its backup was" }
            handler.await { sync_stateQueries.endRestore() }
            throw e
        }
    }

    /** Every preference this device holds that can sync, as the records they are. */
    private suspend fun preferences(database: Database): List<Touched> {
        val profiles = database.sync_local_rowsQueries.profiles().awaitAsList()
        return buildList {
            settings.sharedKeys().forEach { key -> SharedSettings.of(key)?.let { add(Touched(it, it.ref(key))) } }
            profiles.forEach { profile ->
                settings.profileKeys(profile._id).forEach { key ->
                    ProfileSettings.of(key)?.let { add(Touched(it, it.ref(profile._id, key))) }
                }
                settings.sourceKeys(profile._id).forEach { (sourceId, keys) ->
                    keys.forEach { add(Touched(SourceSettingKind, SourceSettingKind.ref(profile._id, sourceId, it))) }
                }
            }
        }
    }

    private suspend fun <P : Any> Database.stamp(
        session: ApplySession,
        reads: LocalReads,
        kind: SyncedKind<P>,
        touched: Touched,
        at: HybridTimestamp,
    ) {
        val stamps = FieldStamps().apply { add(FieldStamps.EVERY_FIELD, at) }
        when (val read = kind.read(reads, touched.ref)) {
            is LocalRead.Found -> {
                val stored = sync_recordsQueries.getByKey(read.key).awaitAsOneOrNull()
                val previous = stored?.takeUnless { it.deleted }?.let { kind.decode(it.payload) }
                val restored = kind.encode(read.build(previous, stamps), stored?.payload)
                session.restored(kind, read.key, touched.ref.json, RecordState.Live(restored))
            }
            LocalRead.Missing -> {
                val key = touched.deletedRecordKey ?: return
                val stored = sync_recordsQueries.getByKey(key).awaitAsOneOrNull()?.takeUnless { it.deleted } ?: return
                val removed = when (kind.absence) {
                    SyncedKind.Absence.TOMBSTONE -> RecordState.Deleted(Tombstone(at, last = stored.payload))
                    SyncedKind.Absence.REMOVED_FLAG ->
                        RecordState.Live(kind.encode(kind.removed(kind.decode(stored.payload), stamps), stored.payload))
                    SyncedKind.Absence.KEEP -> return
                }
                session.restored(kind, key, localRef = null, removed)
            }
            LocalRead.NotSynced -> Unit
        }
    }

    /** A row or preference the restore may have written, and the record it held if the restore deleted it. */
    private class Touched(val kind: SyncedKind<*>, val ref: LocalRef, val deletedRecordKey: String? = null)

    private companion object {
        val kindOrder = SyncKinds.all.withIndex().associate { (index, kind) -> kind.name to index }
    }
}
