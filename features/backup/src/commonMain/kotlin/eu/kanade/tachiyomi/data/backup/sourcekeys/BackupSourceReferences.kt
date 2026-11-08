package eu.kanade.tachiyomi.data.backup.sourcekeys

import eu.kanade.tachiyomi.data.backup.models.Backup
import eu.kanade.tachiyomi.data.backup.models.BackupEntry
import eu.kanade.tachiyomi.data.backup.models.BackupPreference
import eu.kanade.tachiyomi.data.backup.models.BackupSourcePreferences

/**
 * Every source key a backup's settings refer to, which with [BackupEntry.sourceKeys] of its entries are all keys of the
 * device it was made on. The backup lists the identity behind each of them, so a restore can find the same source's key
 * on the device it restores to.
 *
 * Keys inside the JSON of entry feature states aren't read here. Of those, a merge target is also written to
 * [BackupEntry.mergeTargetSource], and the titles a source-supplied merge lists come from its root's source.
 */
internal fun Backup.referencedSourceKeys(): Set<Long> {
    return buildSet {
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

/**
 * [BackupEntry.mergeTargetSource] keeps the backup's key: a backup without merge feature state has it turned into one,
 * and the merge participant translates that state's keys like those of any other.
 */
internal fun BackupEntry.mapSourceKeys(transform: (Long) -> Long) {
    source = transform(source)
}

internal fun BackupSourcePreferences.mapSourceKeys(transform: (Long) -> Long): BackupSourcePreferences {
    return backupSourceKey?.let { copy(sourceId = transform(it)) } ?: this
}
