package app.katari.sync.client.setting.value

import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.katari.sync.client.kind.category.CategoryKind
import app.katari.sync.client.kind.entry.EntryKind
import app.katari.sync.client.payload.SourceReference
import app.katari.sync.client.record.LocalReads
import app.katari.sync.client.record.RecordRef
import tachiyomi.domain.source.identity.SourceIdentity
import tachiyomi.domain.source.identity.SourceKeyedPreferences

/**
 * A preference as it travels: its key and value with every source key in them replaced by an index into a list of the
 * sources as every device knows them, and every local ID of a category or entry by an index into a list of their
 * record keys. The lists are sorted, so two devices that store the same sources under different keys encode the same.
 * Which preferences hold what, and in which format, is the backups' knowledge ([SourceKeyedPreferences]) and
 * [LocalIdPreferences]'.
 */
internal class EncodedSetting(
    val key: String,
    val keySources: List<SourceReference>,
    val value: SettingValue,
    /** The records the value refers to, synced along with it when they never were. */
    val parents: List<RecordRef>,
) {
    /** [keySources] as parts of a record key. */
    val keySourceParts: Array<String> get() = keySources.map { "${it.idScheme}:${it.externalId}" }.toTypedArray()
}

/** Encodes the preference [key] holding [stored] in [profileId]'s stores, or in the shared store when it's null. */
internal suspend fun LocalReads.encodeSetting(key: String, stored: Any?, profileId: Long?): EncodedSetting {
    val keySources = sourcesOf(sourceKeysIn { transform -> SourceKeyedPreferences.mapKey(key, transform) })
    val encodedKey = SourceKeyedPreferences.mapKey(key) { keySources.indexOf(it) }

    val valueSources = sourcesOf(sourceKeysIn { transform -> SourceKeyedPreferences.mapValue(key, stored, transform) })
    val withSources = SourceKeyedPreferences.mapValue(key, stored) { valueSources.indexOf(it) }

    val localIds =
        buildList { LocalIdPreferences.map(key, withSources) { target, id -> id.also { add(target to id) } } }
    val records = localIds.distinct().associateWith { (target, id) -> recordOf(target, id, profileId) }
    val recordKeys = records.values.mapNotNull { it?.first }.distinct().sorted()
    val encoded = LocalIdPreferences.map(key, withSources) { target, id ->
        records[target to id]?.first?.let { recordKeys.indexOf(it).toLong() } ?: UNKNOWN_RECORD
    }
    return EncodedSetting(
        key = encodedKey,
        keySources = keySources.list,
        value = SettingValue.of(encoded, valueSources.list, recordKeys),
        parents = records.values.mapNotNull { it?.second },
    )
}

/** The sources a set of local keys stands for, in the order every device sorts them in. */
private class Sources(private val byLocalKey: Map<Long, SourceReference>) {
    val list = byLocalKey.values.distinct().sortedWith(compareBy({ it.idScheme }, { it.externalId }))

    fun indexOf(localKey: Long): Long = list.indexOf(byLocalKey.getValue(localKey)).toLong()
}

private fun sourceKeysIn(map: ((Long) -> Long) -> Unit): Set<Long> = buildSet { map { key -> key.also(::add) } }

private suspend fun LocalReads.sourcesOf(localKeys: Set<Long>): Sources = Sources(
    localKeys.associateWith { localKey ->
        database.sync_local_rowsQueries.sourceIdentity(localKey).awaitAsOneOrNull()
            ?.let { SourceReference(it.id_scheme, it.external_id) }
            // A key from before the registry, which stands for a tachiyomi ID or the local source, as in backups
            ?: SourceIdentity.ofLegacyKey(localKey).let { SourceReference(it.scheme.key, it.externalId) }
    },
)

/** The record key of the local [id] and the record to sync along with it, or null when it isn't a synced record. */
private suspend fun LocalReads.recordOf(
    target: LocalIdPreferences.Target,
    id: Long,
    profileId: Long?,
): Pair<String, RecordRef?>? = when (target) {
    LocalIdPreferences.Target.CATEGORY -> profileId?.let { categoryKey(id, it) }?.let { key ->
        // The Default category is no record of its own
        key to RecordRef(CategoryKind, CategoryKind.ref(id)).takeIf { id != DEFAULT_CATEGORY_ID }
    }
    LocalIdPreferences.Target.ENTRY -> entryKey(id)?.let { key -> key to RecordRef(EntryKind, EntryKind.ref(id)) }
}

/** Stands for a record this device can't name, such as an entry since removed; it matches nothing anywhere. */
private const val UNKNOWN_RECORD = -1L
private const val DEFAULT_CATEGORY_ID = 0L
