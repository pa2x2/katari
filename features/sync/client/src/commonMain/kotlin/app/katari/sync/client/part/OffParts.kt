package app.katari.sync.client.part

import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.katari.sync.client.kind.entry.EntryKind
import app.katari.sync.client.merge.RecordState
import app.katari.sync.client.record.SyncedKind
import app.katari.sync.protocol.record.SequencedRecord
import kotlinx.serialization.protobuf.ProtoBuf
import tachiyomi.data.Database

/**
 * Whether a record belongs to a part of the account this device chose not to sync: one its kind's choice turns off
 * ([SyncedKind.syncsOn]), or one in a profile the device doesn't sync. A record of an entry is in the entry's profile,
 * found from the entry's record, stored or held; one whose entry isn't here at all counts as synced, and waits for its
 * entry as any record does.
 */
internal class OffParts(val choices: ChoiceSet, private val database: Database) {
    private val entryProfiles = HashMap<String, String?>()

    suspend fun <P : Any> isOff(kind: SyncedKind<P>, payload: P): Boolean {
        if (!kind.syncsOn(choices, payload)) return true
        if (choices.profilesOff.isEmpty()) return false
        val profile = kind.profileOf(payload) ?: kind.entryKeyOf(payload)?.let { entryProfile(it) } ?: return false
        return !choices.syncsProfile(profile)
    }

    /** [isOff] for a record as stored or received; a deletion by the payload it last had, which tells its parts. */
    suspend fun isOff(kind: SyncedKind<*>, deleted: Boolean, bytes: ByteArray): Boolean {
        val payload = lastPayload(RecordState.of(deleted, bytes)) ?: return false
        return decodedIsOff(kind, payload)
    }

    private suspend fun <P : Any> decodedIsOff(kind: SyncedKind<P>, bytes: ByteArray) = isOff(kind, kind.decode(bytes))

    private suspend fun entryProfile(entryKey: String): String? {
        if (entryKey in entryProfiles) return entryProfiles[entryKey]
        val stored = database.sync_recordsQueries.getByKey(entryKey).awaitAsOneOrNull()
        val state = if (stored != null) {
            RecordState.of(stored.deleted, stored.payload)
        } else {
            database.sync_pendingQueries.get(entryKey).awaitAsOneOrNull()
                ?.let { ProtoBuf.decodeFromByteArray(SequencedRecord.serializer(), it.record).record }
                ?.takeIf { it.kind == EntryKind.name && EntryKind.reads(it.kindVersion) }
                ?.let { RecordState.of(it.deleted, it.payload) }
        }
        val profile = state?.let(::lastPayload)?.let { EntryKind.decode(it).profileUuid }
        entryProfiles[entryKey] = profile
        return profile
    }

    private fun lastPayload(state: RecordState): ByteArray? = when (state) {
        is RecordState.Live -> state.bytes
        is RecordState.Deleted -> state.tombstone.last.takeIf { it.isNotEmpty() }
    }
}
