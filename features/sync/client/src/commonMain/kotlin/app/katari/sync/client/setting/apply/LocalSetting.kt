package app.katari.sync.client.setting.apply

import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.katari.sync.client.apply.ApplyOutcome
import app.katari.sync.client.apply.HoldReason
import app.katari.sync.client.apply.LocalWrites
import app.katari.sync.client.apply.Resolved
import app.katari.sync.client.identity.RecordKeys
import app.katari.sync.client.kind.category.CategoryKind
import app.katari.sync.client.kind.entry.EntryKind
import app.katari.sync.client.payload.SourceReference
import app.katari.sync.client.record.LocalReads
import app.katari.sync.client.setting.value.SettingValue
import app.katari.sync.client.setting.value.decodeSetting

/** A preference as this device's store holds it, or why it can't be written yet. */
internal sealed interface LocalSetting {
    class Ready(val key: String, val value: Any?) : LocalSetting

    class NotReady(val outcome: ApplyOutcome) : LocalSetting
}

/**
 * The key and value [profileUuid]'s store (the shared one when null) holds for a preference that traveled as [key],
 * [keySources] and [value] ([decodeSetting]): sources by this device's keys, registered when no extension here has
 * provided them, and categories and entries by their local IDs. One that was deleted stands for nothing, as an ID
 * no device can name does; one that hasn't arrived leaves the preference waiting for it.
 */
internal suspend fun LocalWrites.localSetting(
    key: String,
    keySources: List<SourceReference>,
    value: SettingValue,
    profileUuid: String?,
): LocalSetting {
    val sources = (keySources + value.sources).distinct().associateWith { sourceKey(it) }
    val ids = HashMap<String, Long>()
    value.records.forEach { recordKey ->
        when (val resolved = recordId(recordKey, profileUuid)) {
            is Resolved.Found -> ids[recordKey] = resolved.id
            is Resolved.Missing -> return LocalSetting.NotReady(ApplyOutcome.WaitsFor(resolved.key))
            Resolved.Gone -> ids[recordKey] = NO_RECORD
        }
    }
    val local = decodeSetting(key, keySources, value, sources::get, ids::get)
        ?: return LocalSetting.NotReady(ApplyOutcome.Held(HoldReason.REFERENCE))
    return LocalSetting.Ready(local.first, local.second)
}

private suspend fun LocalWrites.recordId(recordKey: String, profileUuid: String?): Resolved {
    if (profileUuid != null && recordKey == RecordKeys.derive(LocalReads.DEFAULT_CATEGORY, profileUuid)) {
        return Resolved.Found(0)
    }
    val stored = database.sync_recordsQueries.getByKey(recordKey).awaitAsOneOrNull()
    return when (stored?.kind) {
        null -> Resolved.Missing(recordKey)
        EntryKind.name -> entryId(recordKey)
        CategoryKind.name -> profileUuid?.let { categoryId(recordKey, it) } ?: Resolved.Gone
        else -> Resolved.Gone
    }
}

/** An ID no row has, as encoding writes for a record a device can't name. */
private const val NO_RECORD = -1L
