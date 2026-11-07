package eu.kanade.tachiyomi.data.backup.sourcekeys

import eu.kanade.tachiyomi.data.backup.models.BackupPreference
import eu.kanade.tachiyomi.data.backup.models.BackupSourcePreferences
import eu.kanade.tachiyomi.data.backup.models.StringPreferenceValue
import eu.kanade.tachiyomi.data.backup.models.StringSetPreferenceValue
import tachiyomi.domain.source.identity.SourceKeyedPreferences

/** This backed-up preference with the source keys it holds mapped through [transform] ([SourceKeyedPreferences]). */
internal fun BackupPreference.mapSourceKeys(transform: (Long) -> Long): BackupPreference {
    val stored: Any? = when (val value = value) {
        is StringSetPreferenceValue -> value.value
        is StringPreferenceValue -> value.value
        else -> null
    }
    val (mappedKey, mapped) = SourceKeyedPreferences.map(key, stored, transform)
    val mappedValue = when (mapped) {
        stored -> value
        is String -> StringPreferenceValue(mapped)
        is Set<*> -> StringSetPreferenceValue(mapped.mapTo(LinkedHashSet()) { it as String })
        else -> value
    }
    return if (mappedKey == key && mappedValue === value) this else copy(key = mappedKey, value = mappedValue)
}

/** Backups from before the key was written separately only have it in the preference file's name. */
internal val BackupSourcePreferences.backupSourceKey: Long?
    get() = sourceId ?: sourceKey.removePrefix("source_").takeIf { sourceKey.startsWith("source_") }?.toLongOrNull()
