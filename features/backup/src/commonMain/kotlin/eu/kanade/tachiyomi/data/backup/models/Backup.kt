package eu.kanade.tachiyomi.data.backup.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber
import mihon.feature.profiles.core.ProfileScopedBackup

/**
 * A backup file's layout. Reading or writing one leaves the entry lists of it and its profiles empty: BackupReader and
 * BackupWriter hand entries over one at a time instead, as laid out in BackupLayout.
 */
@Serializable
data class Backup(
    @ProtoNumber(1) internal val backupManga: List<LegacyBackupManga> = emptyList(),
    @ProtoNumber(2) var backupCategories: List<BackupCategory> = emptyList(),
    // @ProtoNumber(100) var backupBrokenSources, legacy source model with non-compliant proto number,
    @ProtoNumber(101) var backupSources: List<BackupSource> = emptyList(),
    @ProtoNumber(104) var backupPreferences: List<BackupPreference> = emptyList(),
    @ProtoNumber(105) var backupSourcePreferences: List<BackupSourcePreferences> = emptyList(),
    @ProtoNumber(106) var backupExtensionStores: List<BackupExtensionStore> = emptyList(),
    @ProtoNumber(107) var backupDefaultCategoryUpdateRules: BackupCategoryUpdateRules? = null,
    /** Installs are app-wide, like stores, so they're listed once for every profile. */
    @ProtoNumber(108) var backupFileExtensions: List<BackupFileExtension> = emptyList(),
    @ProtoNumber(200) var backupProfiles: List<ProfileScopedBackup> = emptyList(),
    @ProtoNumber(201) var activeProfileUuid: String? = null,
    @ProtoNumber(202) internal val backupAnime: List<LegacyBackupAnime> = emptyList(),
    @ProtoNumber(203) var backupEntries: List<BackupEntry> = emptyList(),
    // @ProtoNumber(204) var kitsuScoresNative, a tracker score scale marker from before trackers were removed; never reuse
    /**
     * When the backup started reading the data it holds, in epoch milliseconds; 0 in backups made before it was
     * written ([eu.kanade.tachiyomi.data.backup.stream.BackupSummary.createdAt] says what stands in).
     */
    @ProtoNumber(205) var createdAt: Long = 0,
)
