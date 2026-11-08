package app.katari.sync.client.setting.value

import app.katari.sync.client.payload.SourceReference
import tachiyomi.domain.source.identity.SourceKeyedPreferences

/**
 * The key and value a store holds for a preference that traveled as [key], [keySources] and [value]
 * ([encodeSetting]), with this device's keys of the sources ([localSourceKey]) and local IDs of the records
 * ([localId], by record key) put back in; null when one of them isn't on this device yet, so the change waits.
 */
internal fun decodeSetting(
    key: String,
    keySources: List<SourceReference>,
    value: SettingValue,
    localSourceKey: (SourceReference) -> Long?,
    localId: (recordKey: String) -> Long?,
): Pair<String, Any?>? {
    var missing = false
    fun local(index: Long, of: List<SourceReference>): Long =
        of.getOrNull(index.toInt())?.let(localSourceKey) ?: index.also { missing = true }

    val localKey = SourceKeyedPreferences.mapKey(key) { local(it, keySources) }
    val withSources = SourceKeyedPreferences.mapValue(key, value.stored) { local(it, value.sources) }
    val stored = LocalIdPreferences.map(key, withSources) { _, index ->
        // An ID no device could name stays one that matches nothing
        if (index < 0) index else value.records.getOrNull(index.toInt())?.let(localId) ?: index.also { missing = true }
    }
    return if (missing) null else localKey to stored
}
