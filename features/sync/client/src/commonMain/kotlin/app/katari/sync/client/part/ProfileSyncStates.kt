package app.katari.sync.client.part

import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.katari.sync.client.kind.profile.ProfileKind
import app.katari.sync.client.preference.SyncPreferences
import app.katari.sync.protocol.record.SequencedRecord
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.serialization.protobuf.ProtoBuf
import tachiyomi.data.DatabaseHandler

/** Whether this device syncs a profile, or waits for the person to say whether it comes here. */
enum class ProfileSyncState { SYNCED, OFF, ASKING }

/** A profile of this device or of the account, and whether this device syncs it. */
data class ProfileSync(
    val uuid: String,
    val name: String,
    val state: ProfileSyncState,
    /** Whether the profile is on this device; an account profile switched off, or one asking, isn't. */
    val isHere: Boolean,
)

/** The profiles What syncs lists: this device's, then the account's that aren't here, from their records. */
@Inject
class ProfileSyncStates(private val handler: DatabaseHandler, private val preferences: SyncPreferences) {
    fun profiles(): Flow<List<ProfileSync>> = combine(
        handler.subscribeToList { sync_local_rowsQueries.profiles() },
        preferences.profilesOff.changes(),
        preferences.profilesAsking.changes(),
    ) { local, off, asking ->
        val here = local.map { profile ->
            val state = if (profile.uuid in off) ProfileSyncState.OFF else ProfileSyncState.SYNCED
            ProfileSync(profile.uuid, profile.name, state, isHere = true)
        }
        val away = (off + asking - local.mapTo(HashSet()) { it.uuid }).mapNotNull { uuid ->
            val state = if (uuid in asking) ProfileSyncState.ASKING else ProfileSyncState.OFF
            accountName(uuid)?.let { ProfileSync(uuid, it, state, isHere = false) }
        }
        here + away
    }

    /** The name the account's record of the profile [uuid] has, stored or held; null once the account deleted it. */
    private suspend fun accountName(uuid: String): String? = handler.await {
        val key = ProfileKind.key(uuid)
        val stored = sync_recordsQueries.getByKey(key).awaitAsOneOrNull()
        val (deleted, payload) = if (stored != null) {
            stored.deleted to stored.payload
        } else {
            val held = sync_pendingQueries.get(key).awaitAsOneOrNull() ?: return@await null
            val envelope = ProtoBuf.decodeFromByteArray(SequencedRecord.serializer(), held.record).record
            if (!ProfileKind.reads(envelope.kindVersion)) return@await null
            envelope.deleted to envelope.payload
        }
        if (deleted) null else ProfileKind.decode(payload).name.value
    }
}
