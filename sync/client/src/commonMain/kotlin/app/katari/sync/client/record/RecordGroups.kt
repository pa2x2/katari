package app.katari.sync.client.record

import app.katari.sync.client.identity.RecordKeys
import app.katari.sync.client.kind.SyncKinds
import app.katari.sync.client.kind.entry.EntryKind
import app.katari.sync.client.kind.profile.ProfileKind
import app.katari.sync.client.merge.RecordState
import kotlinx.serialization.SerializationException

/**
 * The group a record travels in ([app.katari.sync.protocol.record.RecordEnvelope.group]), which a device fetches ahead
 * of its position: an entry with everything that belongs to it, so a device picked up after another one has the
 * entry's progress first (D052), and the account's profiles, which a first sync pairs this device's with before
 * taking anything else. Group names are computed like keys, so the server learns only which records go together.
 */
object RecordGroups {
    val PROFILES: String = RecordKeys.derive("group.profiles")

    fun entry(entryKey: String): String = entryKey

    /** The group of the record [key] of [kind] as [deleted] and [payload] hold it; empty when it is in none. */
    internal fun of(kind: String, key: String, deleted: Boolean, payload: ByteArray): String {
        val synced = SyncKinds.named(kind) ?: return ""
        if (synced == ProfileKind) return PROFILES
        if (synced == EntryKind) return entry(key)
        val bytes = when (val state = RecordState.of(deleted, payload)) {
            is RecordState.Live -> state.bytes
            is RecordState.Deleted -> state.tombstone.last.takeIf { it.isNotEmpty() } ?: return ""
        }
        return try {
            entryKeyOf(synced, bytes)?.let(::entry).orEmpty()
        } catch (_: SerializationException) {
            ""
        } catch (_: IllegalArgumentException) {
            ""
        }
    }

    private fun <P : Any> entryKeyOf(kind: SyncedKind<P>, bytes: ByteArray): String? = kind.entryKeyOf(
        kind.decode(bytes),
    )
}
