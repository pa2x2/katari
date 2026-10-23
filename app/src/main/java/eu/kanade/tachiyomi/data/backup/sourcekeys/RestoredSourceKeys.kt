package eu.kanade.tachiyomi.data.backup.sourcekeys

import eu.kanade.tachiyomi.data.backup.models.Backup
import eu.kanade.tachiyomi.data.backup.models.toBackupEntry
import tachiyomi.domain.source.identity.SourceIdentity
import tachiyomi.domain.source.identity.SourceIdentityRepository

/**
 * This device's key for each source key a backup refers to, found through the identity the backup lists for it. A key
 * the backup lists no identity for was written before identities were recorded, and stands for a tachiyomi ID or the
 * local source.
 */
class RestoredSourceKeys private constructor(
    private val keys: Map<Long, Long>,
) {
    /** Keys the backup doesn't refer to anywhere aren't expected, and are kept as they are. */
    fun localKey(backupKey: Long): Long = keys[backupKey] ?: backupKey

    /**
     * The backup with this device's keys. Its entries are rewritten in place, after older backups' manga and anime
     * lists are turned into entries, which [Backup.allEntries] would otherwise make anew on every read.
     */
    fun applyTo(backup: Backup): Backup {
        val entries = backup.backupEntries +
            backup.backupManga.map { it.toBackupEntry() } +
            backup.backupAnime.map { it.toBackupEntry() }
        val profiles = backup.backupProfiles.map { profile ->
            profile.copy(
                entries = profile.allEntries(),
                manga = emptyList(),
                anime = emptyList(),
                preferences = profile.preferences.map { it.mapSourceKeys(::localKey) },
                sourcePreferences = profile.sourcePreferences.map { it.mapSourceKeys(::localKey) },
            )
        }
        (entries + profiles.flatMap { it.entries }).forEach { it.mapSourceKeys(::localKey) }
        return backup.copy(
            backupManga = emptyList(),
            backupAnime = emptyList(),
            backupEntries = entries,
            backupSources = backup.backupSources.map { it.copy(sourceId = localKey(it.sourceId)) },
            backupPreferences = backup.backupPreferences.map { it.mapSourceKeys(::localKey) },
            backupSourcePreferences = backup.backupSourcePreferences.map { it.mapSourceKeys(::localKey) },
            backupProfiles = profiles,
        )
    }

    companion object {
        suspend fun of(backup: Backup, identities: SourceIdentityRepository): RestoredSourceKeys {
            val listed = backup.backupSources.associate { it.sourceId to it.identity }
            val keys = (backup.referencedSourceKeys() + listed.keys).associateWith { backupKey ->
                identities.register(listed[backupKey] ?: SourceIdentity.ofLegacyKey(backupKey))
            }
            return RestoredSourceKeys(keys)
        }
    }
}
