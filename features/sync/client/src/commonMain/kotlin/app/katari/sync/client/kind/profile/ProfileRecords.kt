package app.katari.sync.client.kind.profile

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.katari.sync.client.apply.LocalWrites
import app.katari.sync.client.kind.SyncKinds
import app.katari.sync.client.kind.category.CategoryPayload
import app.katari.sync.client.kind.category.CategoryUpdateRulesPayload
import app.katari.sync.client.kind.entry.EntryKind
import app.katari.sync.client.kind.entry.EntryPayload
import app.katari.sync.client.kind.question.QuestionKind
import app.katari.sync.client.kind.setting.ProfileSettingPayload
import app.katari.sync.client.kind.setting.SourceSettingPayload
import app.katari.sync.client.kind.statistics.StatisticsEpochPayload
import app.katari.sync.client.merge.RecordState
import app.katari.sync.client.merge.newestEdit
import app.katari.sync.client.record.SyncedKind
import app.katari.sync.protocol.clock.HybridTimestamp
import tachiyomi.data.Database

/** A record stored for something in a profile, and when it was last edited. */
internal class ProfileRecord(val key: String, val edited: HybridTimestamp?)

/**
 * The live records this device stores for things in the profile [uuid]: its entries and what belongs to them, its
 * categories, settings and statistics, in [SyncKinds.all] order, so a record comes after the ones it refers to.
 */
internal suspend fun profileRecords(database: Database, uuid: String): List<ProfileRecord> {
    val entries = HashSet<String>()
    return SyncKinds.all.filterNot { it === ProfileKind || it is QuestionKind }.flatMap { kind ->
        database.sync_recordsQueries.byKind(kind.name).awaitAsList()
            .filterNot { it.deleted }
            .mapNotNull { record -> kind.inProfile(record.record_key, record.payload, uuid, entries) }
    }
}

/** The profile a record of a kind that names it directly belongs to; null for other kinds. */
internal fun profileOf(payload: Any): String? = when (payload) {
    is EntryPayload -> payload.profileUuid
    is CategoryPayload -> payload.profileUuid
    is CategoryUpdateRulesPayload -> payload.profileUuid
    is StatisticsEpochPayload -> payload.profileUuid
    is ProfileSettingPayload -> payload.profileUuid
    is SourceSettingPayload -> payload.profileUuid
    else -> null
}

private fun <P : Any> SyncedKind<P>.inProfile(
    key: String,
    bytes: ByteArray,
    uuid: String,
    entries: MutableSet<String>,
): ProfileRecord? {
    val payload = decode(bytes)
    val owner = profileOf(payload)
    val belongs = if (owner != null) owner == uuid else entryKeyOf(payload)?.let { it in entries } == true
    if (!belongs) return null
    if (this === EntryKind) entries += key
    return ProfileRecord(key, newestEdit(serializer, payload))
}

/**
 * Brings the profile [uuid] back on this device after it was deleted here, as it was when deleted and with everything
 * this device still stores of it: a profile deleted on one device and changed on another stays until the question
 * about it is answered.
 */
internal suspend fun restoreDeletedProfile(writes: LocalWrites, uuid: String) {
    if (writes.database.sync_applyQueries.profileId(uuid).awaitAsOneOrNull() != null) return
    val key = ProfileKind.key(uuid)
    val stored = writes.database.sync_recordsQueries.getByKey(key).awaitAsOneOrNull() ?: return
    val last = when (val state = RecordState.of(stored.deleted, stored.payload)) {
        is RecordState.Deleted -> state.tombstone.last.takeIf { it.isNotEmpty() } ?: return
        is RecordState.Live -> state.bytes
    }
    ProfileKind.apply(writes, key, ProfileKind.decode(last))
    writes.database.sync_recordsQueries.setLocalRef(localRef = ProfileKind.ref(uuid).json, recordKey = key)
    profileRecords(writes.database, uuid).forEach { writes.reapply(it.key) }
}
