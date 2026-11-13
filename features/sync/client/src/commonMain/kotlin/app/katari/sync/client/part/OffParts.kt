package app.katari.sync.client.part

import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.katari.sync.client.kind.entry.EntryKind
import app.katari.sync.client.kind.profile.ProfileKind
import app.katari.sync.client.merge.RecordState
import app.katari.sync.client.record.SyncedKind
import app.katari.sync.protocol.record.SequencedRecord
import kotlinx.serialization.protobuf.ProtoBuf
import tachiyomi.data.Database

/**
 * Whether a record belongs to a part of the account this device chose not to sync: one its kind's choice turns off
 * ([SyncedKind.syncsOn]), a title's own value of a setting that doesn't sync here ([SyncedKind.overriddenSetting],
 * found through [viewerSettings]), or one in a profile the device doesn't sync. A record of an entry is in the entry's
 * profile, found from the entry's record, stored or held; one whose entry isn't here at all counts as synced, and waits
 * for its entry as any record does.
 *
 * With [ChoiceSet.askBeforeNewProfiles], a profile this device never had is off from its first record on, and [ask]
 * hears of it once, to keep it waiting for the person's answer. A profile [admit]ted is never asked about.
 */
internal class OffParts(
    val choices: ChoiceSet,
    private val database: Database,
    val viewerSettings: ViewerSettingPreferences,
    private val ask: (profileUuid: String) -> Unit = {},
) {
    private val entryProfiles = HashMap<String, String?>()
    private val asked = HashSet<String>()
    private val known = HashSet<String>()

    /**
     * Takes [profile] as one this device has, as the person said it comes here: its records, which waited for that
     * answer while the device never had it, are taken in rather than asked about again.
     */
    fun admit(profile: String) {
        known += profile
    }

    suspend fun <P : Any> isOff(kind: SyncedKind<P>, payload: P): Boolean {
        if (!kind.syncsOn(choices, payload)) return true
        if (kind.overriddenSetting(payload, viewerSettings)?.let(choices::syncsSetting) == false) return true
        if (choices.profilesOff.isEmpty() && !choices.askBeforeNewProfiles) return false
        val profile = profileOf(kind, payload) ?: return false
        return !choices.syncsProfile(profile) || profile in asked || asksAbout(profile)
    }

    /** [isOff] for a record as stored or received; a deletion by the payload it last had, which tells its parts. */
    suspend fun isOff(kind: SyncedKind<*>, deleted: Boolean, bytes: ByteArray): Boolean {
        val payload = lastPayload(RecordState.of(deleted, bytes)) ?: return false
        return decodedIsOff(kind, payload)
    }

    /** The profile a record as stored is in, when it is in one. */
    suspend fun profileOf(kind: SyncedKind<*>, deleted: Boolean, bytes: ByteArray): String? {
        val payload = lastPayload(RecordState.of(deleted, bytes)) ?: return null
        return decodedProfileOf(kind, payload)
    }

    private suspend fun <P : Any> decodedIsOff(kind: SyncedKind<P>, bytes: ByteArray) = isOff(kind, kind.decode(bytes))

    private suspend fun <P : Any> decodedProfileOf(kind: SyncedKind<P>, bytes: ByteArray) =
        profileOf(kind, kind.decode(bytes))

    private suspend fun <P : Any> profileOf(kind: SyncedKind<P>, payload: P): String? =
        kind.profileOf(payload) ?: kind.entryKeyOf(payload)?.let { entryProfile(it) }

    private suspend fun asksAbout(profile: String): Boolean {
        if (!choices.askBeforeNewProfiles || profile == ProfileKind.DEFAULT_UUID || profile in known) return false
        val isNew = database.sync_applyQueries.profileId(profile).awaitAsOneOrNull() == null &&
            database.sync_recordsQueries.getByKey(ProfileKind.key(profile)).awaitAsOneOrNull() == null
        if (isNew) {
            asked += profile
            ask(profile)
        } else {
            known += profile
        }
        return isNew
    }

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
