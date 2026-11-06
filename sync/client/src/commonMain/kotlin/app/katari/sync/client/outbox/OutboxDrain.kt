package app.katari.sync.client.outbox

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOne
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.katari.sync.client.kind.SyncKinds
import app.katari.sync.client.payload.Stamped
import app.katari.sync.client.payload.Tombstone
import app.katari.sync.client.record.FieldStamps
import app.katari.sync.client.record.LocalRead
import app.katari.sync.client.record.LocalReads
import app.katari.sync.client.record.LocalRef
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
 * Turns the outbox into record changes ready to push. Each drained edit gets the hybrid timestamp of when it was made
 * ([EditStamper]); each record it touched is read from its local row and stored in `sync_records` with those
 * timestamps on the fields whose values changed. A record whose fields all ended where they were stored (an edit
 * undone, or a value merged from another device being written locally) changes nothing. Records the payload refers
 * to that were never synced, such as the entry of a chapter marked read, are stored along with it.
 *
 * It only reads the local database and preferences, so it runs whether or not the server is reachable.
 */
@Inject
class OutboxDrain(private val handler: DatabaseHandler, private val settings: SettingReads) {
    /** Drains up to [limit] outbox rows in one transaction and returns how many it drained; 0 once it is empty. */
    suspend fun drain(limit: Long = BATCH): Int = handler.await(inTransaction = true) {
        val state = sync_stateQueries.get().awaitAsOne()
        val deviceId = state.device_id
        if (!state.enabled || deviceId == null) return@await 0
        val rows = sync_outboxQueries.batch(limit).awaitAsList()
        if (rows.isEmpty()) return@await 0

        val clock = sync_clockQueries.get().awaitAsOne()
        val device = DeviceId(deviceId)
        val stamper = EditStamper(device, clock.issued_physical, clock.issued_counter.toInt())
        val drain = Drain(this, LocalReads(this, device, settings))
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

    private class Drain(private val database: Database, private val reads: LocalReads) {
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

        suspend fun addUnsyncedParents() {
            val queue = ArrayDeque(records.values.mapNotNull { it.present })
            while (queue.isNotEmpty()) {
                queue.removeFirst().found.parents.forEach { parent ->
                    if (!read.add(parent.kind.name to parent.ref)) return@forEach
                    database.sync_recordsQueries.getByLocalRef(parent.kind.name, parent.ref.json).awaitAsOneOrNull()
                        ?: touch(parent.kind, parent.ref, deletedRecordKey = null, fields = 0, stamp = null)
                            ?.let(queue::addLast)
                }
            }
        }

        suspend fun store() {
            records.forEach { (key, record) ->
                val present = record.present
                if (present != null) {
                    database.storePresent(key, present, record.stamps)
                } else {
                    database.storeAbsent(key, record.kind, record.stamps)
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

private suspend fun <P : Any> Database.storePresent(key: String, present: Present<P>, stamps: FieldStamps) {
    val kind = present.kind
    val stored = sync_recordsQueries.getByKey(key).awaitAsOneOrNull()?.takeUnless { it.deleted }
    val previous = stored?.let { kind.decode(it.payload) }
    val payload = present.found.build(previous, stamps)
    sync_recordsQueries.detachLocalRef(kind.name, present.ref.json, key)
    val changeId = if (stored != null && payload == previous) {
        if (stored.local_ref == present.ref.json) return
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
    )
}

private suspend fun <P : Any> Database.storeAbsent(key: String, kind: SyncedKind<P>, stamps: FieldStamps) {
    val stored = sync_recordsQueries.getByKey(key).awaitAsOneOrNull() ?: return
    if (stored.deleted) return
    when (kind.absence) {
        SyncedKind.Absence.TOMBSTONE -> sync_recordsQueries.storeLocalChange(
            recordKey = key,
            kind = kind.name,
            localRef = null,
            kindVersion = kind.version.toLong(),
            payload = ProtoBuf.encodeToByteArray(
                Tombstone.serializer(),
                Tombstone(stamps.latest ?: Stamped.UNKNOWN_EDIT, last = stored.payload),
            ),
            deleted = true,
            changeId = newChangeId(),
        )
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
            )
        }
        SyncedKind.Absence.KEEP -> Unit
    }
}
