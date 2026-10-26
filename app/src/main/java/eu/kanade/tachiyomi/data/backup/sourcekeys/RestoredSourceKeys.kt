package eu.kanade.tachiyomi.data.backup.sourcekeys

import eu.kanade.tachiyomi.data.backup.models.Backup
import eu.kanade.tachiyomi.data.backup.models.BackupEntry
import eu.kanade.tachiyomi.data.backup.stream.BackupSummary
import mihon.entry.interactions.persistence.backup.EntryBackupSourceKeys
import tachiyomi.domain.source.identity.SourceIdentity
import tachiyomi.domain.source.identity.SourceIdentityRepository

/**
 * This device's key for each source key a backup refers to, found through the identity the backup lists for it. A key
 * the backup lists no identity for was written before identities were recorded, and stands for a tachiyomi ID or the
 * local source. Entry feature states keep the backup's keys and are translated by their participants through this.
 */
class RestoredSourceKeys private constructor(
    private val keys: Map<Long, Long>,
) : EntryBackupSourceKeys {
    /** Keys the backup doesn't refer to anywhere aren't expected, and are kept as they are. */
    override fun localKey(backupKey: Long): Long = keys[backupKey] ?: backupKey

    /** [metadata], a backup read without its entries, with this device's keys. */
    fun applyTo(metadata: Backup): Backup {
        return metadata.copy(
            backupSources = metadata.backupSources.map { it.copy(sourceId = localKey(it.sourceId)) },
            backupPreferences = metadata.backupPreferences.map { it.mapSourceKeys(::localKey) },
            backupSourcePreferences = metadata.backupSourcePreferences.map { it.mapSourceKeys(::localKey) },
            backupProfiles = metadata.backupProfiles.map { profile ->
                profile.copy(
                    preferences = profile.preferences.map { it.mapSourceKeys(::localKey) },
                    sourcePreferences = profile.sourcePreferences.map { it.mapSourceKeys(::localKey) },
                )
            },
        )
    }

    fun applyTo(entry: BackupEntry) {
        entry.mapSourceKeys(::localKey)
    }

    companion object {
        internal suspend fun of(summary: BackupSummary, identities: SourceIdentityRepository): RestoredSourceKeys {
            val listed = summary.metadata.backupSources.associate { it.sourceId to it.identity }
            val referenced = summary.metadata.referencedSourceKeys() + summary.entrySourceKeys
            val keys = (referenced + listed.keys).associateWith { backupKey ->
                identities.register(listed[backupKey] ?: SourceIdentity.ofLegacyKey(backupKey))
            }
            return RestoredSourceKeys(keys)
        }
    }
}
