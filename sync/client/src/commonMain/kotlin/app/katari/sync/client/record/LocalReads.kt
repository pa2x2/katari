package app.katari.sync.client.record

import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.katari.sync.client.identity.RecordKeys
import app.katari.sync.data.EntryRow
import app.katari.sync.protocol.device.DeviceId
import tachiyomi.data.Database

/**
 * Reads of local rows during one drain, with the keys of the records rows refer to. Keys found or assigned here are
 * remembered for the rest of the drain, so a category given a random key keeps it when its memberships refer to it.
 */
class LocalReads internal constructor(
    val database: Database,
    /** This device, whose own share of running totals a record holds apart from other devices' shares. */
    val device: DeviceId,
) {
    private val entries = HashMap<Long, EntryRow?>()
    private val profileUuids = HashMap<Long, String?>()
    private val randomKeys = HashMap<Pair<String, LocalRef>, String>()

    suspend fun entry(entryId: Long): EntryRow? = entries.getOrPut(entryId) {
        database.sync_local_rowsQueries.entryRow(entryId).awaitAsOneOrNull()
    }

    /** The key of the entry, or null when it is gone or never syncs (local-source entries). */
    suspend fun entryKey(entryId: Long): String? = entry(entryId)?.let(::entryKeyOf)

    fun entryKeyOf(entry: EntryRow): String? = entryKey(
        profileUuid = entry.profile_uuid,
        idScheme = entry.id_scheme,
        externalId = entry.external_id,
        url = entry.url,
        type = entry.type,
    )

    fun entryKey(profileUuid: String, idScheme: String, externalId: Long, url: String, type: String): String? {
        if (idScheme == LOCAL_SCHEME) return null
        return RecordKeys.derive(ENTRY, profileUuid, idScheme, externalId.toString(), url, type)
    }

    suspend fun chapterKey(chapterId: Long): String? {
        val chapter = database.sync_local_rowsQueries.chapterRow(chapterId).awaitAsOneOrNull() ?: return null
        return chapterKey(entryKey(chapter.entry_id) ?: return null, chapter.url)
    }

    fun chapterKey(entryKey: String, url: String): String = RecordKeys.derive(CHAPTER, entryKey, url)

    suspend fun profileUuid(profileId: Long): String? = profileUuids.getOrPut(profileId) {
        database.sync_local_rowsQueries.profileUuid(profileId).awaitAsOneOrNull()
    }

    /**
     * The key of a category. The Default category is one row (`_id = 0`) every profile shares and nobody edits, so
     * it is not a record: each profile's Default has a key computed from the profile, which is the same on every
     * device, for the records that refer to it. Other categories were made by a person and have random keys.
     */
    suspend fun categoryKey(categoryId: Long, profileId: Long): String? {
        if (categoryId == DEFAULT_CATEGORY_ID) {
            return RecordKeys.derive(DEFAULT_CATEGORY, profileUuid(profileId) ?: return null)
        }
        return randomKey(CATEGORY, LocalRef.of(categoryId))
    }

    /** The key a record of [kind] made by a person has: the one it was stored with, or a new random one. */
    suspend fun randomKey(kind: String, ref: LocalRef): String = randomKeys.getOrPut(kind to ref) {
        database.sync_recordsQueries.getByLocalRef(kind, ref.json).awaitAsOneOrNull()?.record_key
            ?: RecordKeys.random()
    }

    companion object {
        const val ENTRY = "entry"
        const val CHAPTER = "chapter"
        const val CATEGORY = "category"
        const val DEFAULT_CATEGORY = "category.default"
        private const val LOCAL_SCHEME = "local"
        private const val DEFAULT_CATEGORY_ID = 0L
    }
}
