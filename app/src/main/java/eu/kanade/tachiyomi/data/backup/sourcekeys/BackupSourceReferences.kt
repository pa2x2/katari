package eu.kanade.tachiyomi.data.backup.sourcekeys

import eu.kanade.tachiyomi.data.backup.models.Backup
import eu.kanade.tachiyomi.data.backup.models.BackupEntry
import eu.kanade.tachiyomi.data.backup.models.BackupPreference
import eu.kanade.tachiyomi.data.backup.models.BackupSourcePreferences

/**
 * Every source key a backup refers to, which are all keys of the device it was made on. The backup lists the identity
 * behind each of them, so a restore can find the same source's key on the device it restores to.
 *
 * Not covered yet: entry states a feature backs up as its own JSON, such as the target of a merged entry, which a
 * restore reads before the legacy [BackupEntry.mergeTargetSource] it maps.
 */
internal fun Backup.referencedSourceKeys(): Set<Long> {
    return buildSet {
        allEntries().forEach { entry -> entry.sourceKeys().forEach(::add) }
        backupSourcePreferences.mapNotNullTo(this) { it.backupSourceKey }
        backupPreferences.flatMapTo(this) { it.sourceKeys() }
        backupProfiles.forEach { profile ->
            profile.sourcePreferences.mapNotNullTo(this) { it.backupSourceKey }
            profile.preferences.flatMapTo(this) { it.sourceKeys() }
        }
    }
}

internal fun BackupEntry.sourceKeys(): List<Long> = listOfNotNull(source, mergeTargetSource)

private fun BackupPreference.sourceKeys(): List<Long> = buildList { mapSourceKeys { key -> key.also(::add) } }

internal fun BackupEntry.mapSourceKeys(transform: (Long) -> Long) {
    source = transform(source)
    mergeTargetSource = mergeTargetSource?.let(transform)
}

internal fun BackupSourcePreferences.mapSourceKeys(transform: (Long) -> Long): BackupSourcePreferences {
    return backupSourceKey?.let { copy(sourceId = transform(it)) } ?: this
}
