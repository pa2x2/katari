package app.katari.sync.client.restore

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOne
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.katari.sync.client.apply.ApplySession
import app.katari.sync.client.apply.SyncWriting
import app.katari.sync.client.kind.SyncKinds
import app.katari.sync.client.kind.setting.ProfileSettingKind
import app.katari.sync.client.kind.setting.SharedSettingKind
import app.katari.sync.client.kind.setting.SourceSettingKind
import app.katari.sync.client.merge.RecordState
import app.katari.sync.client.payload.Tombstone
import app.katari.sync.client.record.FieldStamps
import app.katari.sync.client.record.LocalRead
import app.katari.sync.client.record.LocalReads
import app.katari.sync.client.record.LocalRef
import app.katari.sync.client.record.SyncedKind
import app.katari.sync.client.setting.capture.SettingsCapture
import app.katari.sync.client.setting.read.SettingReads
import app.katari.sync.protocol.clock.HybridTimestamp
import app.katari.sync.protocol.device.DeviceId
import dev.zacsweers.metro.Inject
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
 * hearing of the rollback wins over it again. Emptying and the confirmation are the backup feature's and unit 10's.
 */
@Inject
class RestoreStamping(
    private val handler: DatabaseHandler,
    private val writing: SyncWriting,
    private val capture: SettingsCapture,
    private val settings: SettingReads,
) {
    /**
     * Runs [restore], which restores a backup created at [backupCreatedAt] (epoch milliseconds), and records what it
     * wrote as changes made then. Without sync it only runs [restore].
     *
     * Preference edits are not captured while [restore] runs; every person-level preference is compared with this
     * device's copy afterwards instead, since a store's listeners may hear a restore's writes only after it returns
     * and would record them as edits made then.
     */
    suspend fun <T> restoreOnThisDevice(backupCreatedAt: Long, restore: suspend () -> T): T {
        val state = handler.await { sync_stateQueries.get().awaitAsOne() }
        if (!state.enabled || state.device_id == null) return restore()
        capture.flush()
        capture.stop()
        val before = handler.await { sync_outboxQueries.lastSeq().awaitAsOne() }
        val result = try {
            restore()
        } finally {
            capture.start()
        }
        writing.write(takeAfter = before) { session, taken ->
            val at = HybridTimestamp(backupCreatedAt, 0, DeviceId(checkNotNull(state.device_id)))
            val reads = LocalReads(this, DeviceId(checkNotNull(state.device_id)), settings)
            val touched = taken.groupBy { it.kind to it.local_ref }
                .mapNotNull { (located, rows) ->
                    val kind = SyncKinds.named(located.first) ?: return@mapNotNull null
                    Touched(kind, LocalRef(located.second), rows.firstNotNullOfOrNull { it.record_key })
                }
            (touched + preferences(this))
                .sortedBy { kindOrder.getValue(it.kind.name) }
                .forEach { stamp(session, reads, it.kind, it, at) }
        }
        return result
    }

    /** Every person-level preference this device holds, as the records they are. */
    private suspend fun preferences(database: Database): List<Touched> {
        val profiles = database.sync_local_rowsQueries.profiles().awaitAsList()
        return buildList {
            settings.sharedKeys().forEach { add(Touched(SharedSettingKind, SharedSettingKind.ref(it))) }
            profiles.forEach { profile ->
                settings.profileKeys(profile._id).forEach {
                    add(Touched(ProfileSettingKind, ProfileSettingKind.ref(profile._id, it)))
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
