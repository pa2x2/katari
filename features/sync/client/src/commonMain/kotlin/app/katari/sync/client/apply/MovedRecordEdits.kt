package app.katari.sync.client.apply

import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.katari.sync.client.merge.RecordState
import app.katari.sync.client.part.OffParts
import app.katari.sync.client.payload.Tombstone
import app.katari.sync.client.record.RecordGroups
import app.katari.sync.client.record.SyncedKind
import app.katari.sync.client.record.newChangeId

/**
 * Edits of a record whose row moved to another key ([SyncedKind.moveInto]), as a refresh found a chapter at a new
 * URL, made at the old key by a device that hadn't heard of the move yet: a chapter marked read, or a position read
 * further. The device that moved the row deletes the old key's record in favour of the new one
 * ([app.katari.sync.client.outbox.OutboxDrain]); the edits go to the new one, merged field by field like any edit,
 * instead of being lost with the old key or making the record live there again. Each device carries the same edits to
 * the same record, so the changes it pushes merge into one.
 */
internal class MovedRecordEdits(private val writes: LocalWrites, private val parts: OffParts) {

    /**
     * The key of the record that replaced the one [local] and [incoming] are copies of, when one of them is its
     * deletion in favour of it and the other holds edits, and the replacement isn't here yet: the record waits for
     * it, so the edits stay until they have somewhere to go.
     */
    suspend fun <P : Any> waitsFor(kind: SyncedKind<P>, local: RecordState?, incoming: RecordState): String? {
        val crossing = crossing(kind, local, incoming) ?: return null
        return (writes.database.standing(crossing.replacement) as? Standing.Missing)?.key
    }

    /**
     * [merged], or the record kept deleted when it holds edits made where the row was, after the move, by a device that
     * hadn't heard of it. The deletion is stamped when the move was made, so those edits would make the record live
     * again, and with it the row at its old key on every device; it is stamped anew instead, after them, and the edits
     * go where the row moved ([carry]).
     */
    suspend fun <P : Any> settled(
        kind: SyncedKind<P>,
        local: RecordState?,
        incoming: RecordState,
        merged: RecordState,
    ): RecordState {
        if (merged !is RecordState.Live) return merged
        val crossing = crossing(kind, local, incoming) ?: return merged
        val target = writes.database.standing(crossing.replacement) as? Standing.Live ?: return merged
        val edits = kind.decode(crossing.edits.bytes)
        if (!kind.editedBeforeHeardOfMove(edits, kind.decode(target.record.payload))) return merged
        return RecordState.Deleted(
            Tombstone(writes.newEdit(), replacedBy = crossing.replacement, last = crossing.edits.bytes),
        )
    }

    /** Carries to the replacement the edits of [local] or [incoming] that [merged], the two merged, deleted. */
    suspend fun <P : Any> carry(kind: SyncedKind<P>, local: RecordState?, incoming: RecordState, merged: RecordState) {
        val moveInto = kind.moveInto ?: return
        if (merged !is RecordState.Deleted) return
        val crossing = crossing(kind, local, incoming) ?: return
        val target = writes.database.standing(crossing.replacement) as? Standing.Live ?: return
        val edits = moveInto(kind.decode(crossing.edits.bytes), kind.decode(target.record.payload))
        writes.change(kind, target.key, edits)
    }

    /**
     * Moves to [key] the live record of the same entry that held the row [ref] under another key, when writing [key]
     * took the row over: another device's refresh moved the row, and this device moves the record as its own refresh
     * would. The old key is deleted in favour of [key] after every edit made here at it, which [key] takes on.
     */
    suspend fun <P : Any> takeOver(kind: SyncedKind<P>, key: String, ref: String) {
        val moveInto = kind.moveInto ?: return
        val queries = writes.database.sync_recordsQueries
        val held = queries.getByLocalRef(kind.name, ref).awaitAsOneOrNull() ?: return
        if (held.deleted || held.record_key == key) return
        val target = queries.getByKey(key).awaitAsOneOrNull()?.takeUnless { it.deleted } ?: return
        val edits = kind.decode(held.payload)
        val replacement = kind.decode(target.payload)
        if (kind.entryKeyOf(edits) != kind.entryKeyOf(replacement)) return

        val tombstone = RecordState.Deleted(Tombstone(writes.newEdit(), replacedBy = key, last = held.payload)).bytes
        queries.storeLocalChange(
            recordKey = held.record_key,
            kind = kind.name,
            localRef = null,
            kindVersion = kind.version.toLong(),
            payload = tombstone,
            deleted = true,
            changeId = newChangeId(),
            recordGroup = RecordGroups.of(kind.name, held.record_key, deleted = true, tombstone),
        )
        if (parts.isOff(kind, deleted = true, tombstone)) queries.holdBack(held.record_key)
        writes.change(kind, key, moveInto(edits, replacement))
    }

    /** One of [local] and [incoming] deleted in favour of another record as its row moved, the other with edits. */
    private fun <P : Any> crossing(kind: SyncedKind<P>, local: RecordState?, incoming: RecordState): Crossing? {
        if (kind.moveInto == null || local == null) return null
        val (deleted, edits) = when {
            local is RecordState.Deleted && incoming is RecordState.Live -> local to incoming
            local is RecordState.Live && incoming is RecordState.Deleted -> incoming to local
            else -> return null
        }
        return deleted.tombstone.replacedBy?.let { Crossing(it, edits) }
    }

    private class Crossing(val replacement: String, val edits: RecordState.Live)
}
