package app.katari.sync.client.outbox

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOne
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.katari.sync.client.kind.SyncKinds
import app.katari.sync.client.part.OffParts
import app.katari.sync.client.payload.Stamped
import app.katari.sync.client.payload.Tombstone
import app.katari.sync.client.preference.SyncPreferences
import app.katari.sync.client.record.FieldStamps
import app.katari.sync.client.record.LocalRead
import app.katari.sync.client.record.LocalReads
import app.katari.sync.client.record.LocalRef
import app.katari.sync.client.record.RecordGroups
import app.katari.sync.client.record.SyncedKind
import app.katari.sync.client.record.newChangeId
import app.katari.sync.client.setting.read.SettingReads
import app.katari.sync.protocol.clock.HybridTimestamp
import app.katari.sync.protocol.device.DeviceId
import dev.zacsweers.metro.Inject
import kotlinx.serialization.protobuf.ProtoBuf
import tachiyomi.data.Database
import tachiyomi.data.DatabaseHandler

/**
 * Turns the outbox into record changes ready to push, after queueing the records stored under an older version of
 * their kind to be read again ([SyncedKind.oldestReadableVersion]). Each drained edit gets the hybrid timestamp of
 * when it was made ([EditStamper]); each record it touched is read from its local row and stored in `sync_records`
 * with those timestamps on the fields whose values changed. A record whose fields all ended where they were stored
 * (an edit undone, or a value merged from another device being written locally) changes nothing. Records the payload
 * refers to that were never synced, such as the entry of a chapter marked read, are stored along with it.
 *
 * A record of a part of the account this device chose not to sync ([OffParts]) is stored like any other, so its edits
 * keep the times they were made, and held back from pushing until the part is synced again.
 *
 * It only reads the local database and preferences, so it runs whether or not the server is reachable. The rows a
 * restore under way wrote stay in the outbox for the restore to record
 * ([app.katari.sync.client.restore.RestoreStamping]).
 */
@Inject
class OutboxDrain(
    private val handler: DatabaseHandler,
    private val settings: SettingReads,
    private val preferences: SyncPreferences,
) {
    /**
     * Drains up to [limit] outbox rows in one transaction and returns how many it drained; 0 once it is empty.
     *
     * A record that holds a drained row under a key the row no longer has lets go of the row, or, when [replacing],
     * is removed without being marked deleted. A device returning to its account with its records replaces them so:
     * a profile paired by name with an account profile of another identity changes every key under it, and the
     * device must neither push its records under the old keys nor carry their contents over to the new ones (D066).
     * Pairing never gives a profile an identity another of the device's profiles had, so no row takes a key another
     * row's record still holds.
     */
    suspend fun drain(limit: Long = BATCH, replacing: Boolean = false): Int = handler.await(inTransaction = true) {
        val state = sync_stateQueries.get().awaitAsOne()
        val deviceId = state.device_id
        if (!state.enabled || deviceId == null) return@await 0
        val through = state.restore_outbox_seq
        // Rows queued now would be the restore's too, and recorded with its rows as what they stand for
        if (through == null) queueOutdatedRereads()
        val rows = sync_outboxQueries.batch(limit).awaitAsList().takeWhile { through == null || it.seq <= through }
        if (rows.isEmpty()) return@await 0

        val clock = sync_clockQueries.get().awaitAsOne()
        val device = DeviceId(deviceId)
        val stamper = EditStamper(device, clock.issued_physical, clock.issued_counter.toInt())
        val parts = OffParts(preferences.choices(), this)
        val drain = Drain(this, LocalReads(this, device, settings), parts, replacing)
        rows.forEach { row ->
            val stamp = stamper.stamp(row.changed_at, row.seen_physical, row.seen_counter.toInt())
            val kind = SyncKinds.named(row.kind) ?: return@forEach
            drain.touch(kind, LocalRef(row.local_ref), row.record_key, row.fields, stamp)
        }
        drain.addUnsyncedParents()
        drain.store()

        sync_outboxQueries.deleteThrough(rows.last().seq)
        sync_clockQueries.setIssued(stamper.issuedPhysical, stamper.issuedCounter.toLong())
        rows.size
    }

    private suspend fun Database.queueOutdatedRereads() {
        SyncKinds.all.filter { it.oldestReadableVersion < it.version }.forEach { kind ->
            val version = kind.version.toLong()
            // Inserting no rows still tells the outbox's listeners it changed, and that starts another drain: a drain
            // that queued nothing would start the next one, for as long as the app is open
            if (!sync_recordsQueries.hasOutdated(kind = kind.name, version = version).awaitAsOne()) return@forEach
            sync_outboxQueries.recordRereadOfOutdated(kind = kind.name, version = version)
            sync_recordsQueries.raiseVersion(kind = kind.name, version = version)
        }
    }

    private class Drain(
        private val database: Database,
        private val reads: LocalReads,
        private val parts: OffParts,
        private val replacing: Boolean,
    ) {
        private val records = LinkedHashMap<String, Touched>()
        private val read = HashSet<Pair<String, LocalRef>>()

        /** Records an edit of the row [ref] locates and returns the record it holds, when it holds one now. */
        suspend fun <P : Any> touch(
            kind: SyncedKind<P>,
            ref: LocalRef,
            deletedRecordKey: String?,
            fields: Long,
            stamp: HybridTimestamp?,
        ): Present<P>? {
            if (deletedRecordKey != null) {
                record(deletedRecordKey, kind).add(fields, stamp)
                return null
            }
            read += kind.name to ref
            return when (val found = kind.read(reads, ref)) {
                is LocalRead.Found -> Present(kind, ref, found).also { present ->
                    record(found.key, kind).apply {
                        this.present = present
                        add(fields, stamp)
                    }
                }
                LocalRead.Missing -> {
                    database.sync_recordsQueries.getByLocalRef(kind.name, ref.json)
                        .awaitAsOneOrNull()
                        ?.let { record(it.record_key, kind).add(fields, stamp) }
                    null
                }
                LocalRead.NotSynced -> null
            }
        }

        /**
         * Stores the parents of the records drained that have no record of their own yet. A parent's row can be held by
         * a record under another key, as a chapter's is by its entry's old key once the entry moved to another
         * profile; that record no longer stands for the row, so the parent is stored under the key it has now.
         */
        suspend fun addUnsyncedParents() {
            val queue = ArrayDeque(records.values.mapNotNull { it.present })
            while (queue.isNotEmpty()) {
                queue.removeFirst().found.parents.forEach { parent ->
                    if (!read.add(parent.kind.name to parent.ref)) return@forEach
                    val held = database.sync_recordsQueries.getByLocalRef(parent.kind.name, parent.ref.json)
                        .awaitAsOneOrNull()
                    if (held != null) {
                        val current = currentKey(parent.kind, parent.ref)
                        if (current == null || current == held.record_key) return@forEach
                    }
                    touch(parent.kind, parent.ref, deletedRecordKey = null, fields = 0, stamp = null)
                        ?.let(queue::addLast)
                }
            }
        }

        private suspend fun currentKey(kind: SyncedKind<*>, ref: LocalRef): String? =
            (kind.read(reads, ref) as? LocalRead.Found)?.key

        suspend fun store() {
            records.forEach { (key, record) ->
                val present = record.present
                if (present != null) {
                    database.storePresent(key, present, record.stamps, parts, replacing)
                } else {
                    database.storeAbsent(key, record.kind, record.stamps, parts)
                }
            }
        }

        private fun record(key: String, kind: SyncedKind<*>): Touched = records.getOrPut(key) { Touched(kind) }
    }

    private companion object {
        const val BATCH = 500L
    }
}

private class Touched(val kind: SyncedKind<*>) {
    val stamps = FieldStamps()
    var present: Present<*>? = null

    fun add(fields: Long, stamp: HybridTimestamp?) {
        if (stamp != null) stamps.add(fields, stamp)
    }
}

private class Present<P : Any>(val kind: SyncedKind<P>, val ref: LocalRef, val found: LocalRead.Found<P>)

private suspend fun <P : Any> Database.storePresent(
    key: String,
    present: Present<P>,
    stamps: FieldStamps,
    parts: OffParts,
    replacing: Boolean,
) {
    val kind = present.kind
    val stored = sync_recordsQueries.getByKey(key).awaitAsOneOrNull()?.takeUnless { it.deleted }
    val previous = stored?.let { kind.decode(it.payload) }
    val payload = present.found.build(previous, stamps)
    // A row is held by one record at most, so when this one holds it no other is left to let it go
    val holdsRow = stored?.local_ref == present.ref.json
    if (!holdsRow) {
        if (replacing) {
            sync_recordsQueries.deleteReplaced(kind.name, present.ref.json, key)
        } else {
            sync_recordsQueries.detachLocalRef(kind.name, present.ref.json, key)
        }
    }
    val changeId = if (stored != null && payload == previous) {
        if (holdsRow) return
        stored.change_id
    } else {
        newChangeId()
    }
    sync_recordsQueries.storeLocalChange(
        recordKey = key,
        kind = kind.name,
        localRef = present.ref.json,
        kindVersion = kind.version.toLong(),
        payload = kind.encode(payload, stored?.payload),
        deleted = false,
        changeId = changeId,
        recordGroup = RecordGroups.of(kind, key, payload),
    )
    if (parts.isOff(kind, payload)) sync_recordsQueries.holdBack(key)
}

private suspend fun <P : Any> Database.storeAbsent(
    key: String,
    kind: SyncedKind<P>,
    stamps: FieldStamps,
    parts: OffParts,
) {
    val stored = sync_recordsQueries.getByKey(key).awaitAsOneOrNull() ?: return
    if (stored.deleted) return
    when (kind.absence) {
        SyncedKind.Absence.TOMBSTONE -> {
            val tombstone = ProtoBuf.encodeToByteArray(
                Tombstone.serializer(),
                Tombstone(stamps.latest ?: Stamped.UNKNOWN_EDIT, last = stored.payload),
            )
            sync_recordsQueries.storeLocalChange(
                recordKey = key,
                kind = kind.name,
                localRef = null,
                kindVersion = kind.version.toLong(),
                payload = tombstone,
                deleted = true,
                changeId = newChangeId(),
                recordGroup = RecordGroups.of(kind.name, key, deleted = true, tombstone),
            )
            if (parts.isOff(kind, kind.decode(stored.payload))) sync_recordsQueries.holdBack(key)
        }
        SyncedKind.Absence.REMOVED_FLAG -> {
            val previous = kind.decode(stored.payload)
            val removed = kind.removed(previous, stamps)
            if (removed == previous && stored.local_ref == null) return
            sync_recordsQueries.storeLocalChange(
                recordKey = key,
                kind = kind.name,
                localRef = null,
                kindVersion = kind.version.toLong(),
                payload = kind.encode(removed, stored.payload),
                deleted = false,
                changeId = if (removed == previous) stored.change_id else newChangeId(),
                recordGroup = RecordGroups.of(kind, key, removed),
            )
            if (parts.isOff(kind, removed)) sync_recordsQueries.holdBack(key)
        }
        SyncedKind.Absence.KEEP -> Unit
    }
}
