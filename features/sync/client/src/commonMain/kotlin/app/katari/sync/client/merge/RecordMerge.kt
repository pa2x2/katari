package app.katari.sync.client.merge

import app.katari.sync.client.payload.Tombstone
import app.katari.sync.client.record.SyncedKind
import kotlinx.serialization.protobuf.ProtoBuf

/** A record as a device holds or receives it: a payload, or a deletion. */
internal sealed interface RecordState {
    val deleted: Boolean
    val bytes: ByteArray

    class Live(override val bytes: ByteArray) : RecordState {
        override val deleted = false
    }

    class Deleted(val tombstone: Tombstone) : RecordState {
        override val deleted = true
        override val bytes: ByteArray get() = ProtoBuf.encodeToByteArray(Tombstone.serializer(), tombstone)
    }

    companion object {
        fun of(deleted: Boolean, payload: ByteArray): RecordState = if (deleted) {
            Deleted(ProtoBuf.decodeFromByteArray(Tombstone.serializer(), payload))
        } else {
            Live(payload)
        }
    }
}

/**
 * The record holding both [local]'s and [incoming]'s changes ([SyncedKind.merge] for two payloads).
 *
 * A deletion wins over a payload last edited before it and loses to one edited after it. The record that loses is
 * forgotten whole: a field edited after the deletion brings the record back as the device that edited it held it,
 * every field included, and a payload last edited before the deletion leaves nothing behind, not even its fields'
 * timestamps. That is the same whichever device merges, so they agree on whether the record exists and on its fields.
 *
 * Fields a newer app version added are kept from [incoming]: it is the server's newer copy, which holds everything
 * any device merged into the record before, while this app never edits fields it doesn't know.
 */
internal fun <P : Any> SyncedKind<P>.merge(local: RecordState, incoming: RecordState): RecordState = when {
    local is RecordState.Live && incoming is RecordState.Live -> {
        val merged = merge(decode(local.bytes), decode(incoming.bytes))
        RecordState.Live(encode(merged, previous = incoming.bytes))
    }
    local is RecordState.Deleted && incoming is RecordState.Deleted ->
        if (incoming.tombstone.deletedAt > local.tombstone.deletedAt) incoming else local
    local is RecordState.Live && incoming is RecordState.Deleted -> survivor(local, incoming)
    local is RecordState.Deleted && incoming is RecordState.Live -> survivor(incoming, local)
    else -> error("Unreachable")
}

private fun <P : Any> SyncedKind<P>.survivor(live: RecordState.Live, deletion: RecordState.Deleted): RecordState {
    val edited = newestEdit(serializer, decode(live.bytes))
    return if (edited != null && edited > deletion.tombstone.deletedAt) live else deletion
}
