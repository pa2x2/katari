package mihon.feature.profiles.core

import eu.kanade.tachiyomi.data.backup.models.BackupCategory
import eu.kanade.tachiyomi.data.backup.models.BackupEntry
import eu.kanade.tachiyomi.data.backup.models.BackupPreference
import eu.kanade.tachiyomi.data.backup.models.BackupSourcePreferences
import eu.kanade.tachiyomi.data.backup.models.LegacyBackupAnime
import eu.kanade.tachiyomi.data.backup.models.LegacyBackupManga
import eu.kanade.tachiyomi.data.backup.models.toBackupEntry
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
data class ProfileScopedBackup(
    @ProtoNumber(1) val profile: ProfileBackup,
    @ProtoNumber(2) val categories: List<BackupCategory> = emptyList(),
    @ProtoNumber(3) val manga: List<LegacyBackupManga> = emptyList(),
    @ProtoNumber(4) val preferences: List<BackupPreference> = emptyList(),
    @ProtoNumber(5) val sourcePreferences: List<BackupSourcePreferences> = emptyList(),
    @ProtoNumber(6) val anime: List<LegacyBackupAnime> = emptyList(),
    @ProtoNumber(7) val entries: List<BackupEntry> = emptyList(),
) {
    fun allEntries(): List<BackupEntry> {
        return entries + manga.map { it.toBackupEntry() } + anime.map { it.toBackupEntry() }
    }
}

@Serializable
data class ProfileBackup(
    @ProtoNumber(1) val uuid: String,
    @ProtoNumber(2) val name: String,
    @ProtoNumber(3) val colorSeed: Long,
    @ProtoNumber(4) val position: Long,
    @ProtoNumber(5) val requiresAuth: Boolean,
    @ProtoNumber(6) val isArchived: Boolean,
    @ProtoNumber(7) val type: ProfileType? = null,
)

/**
 * Legacy profile type serialized into older backups.
 *
 * Profiles are now untyped containers, so the value is only preserved for
 * backward compatibility and ignored during restore.
 */
@Serializable
enum class ProfileType {
    MANGA,
    ANIME,
}
