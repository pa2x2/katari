package eu.kanade.tachiyomi.data.backup.stream

import app.katari.profiles.core.ProfileScopedBackup
import eu.kanade.tachiyomi.data.backup.models.Backup
import eu.kanade.tachiyomi.data.backup.models.BackupEntry

/** How an entry is encoded: as a [BackupEntry], or as the manga and anime lists of backups before entries. */
internal enum class BackupEntryFormat {
    ENTRY,
    LEGACY_MANGA,
    LEGACY_ANIME,
}

/**
 * Where entries sit in an encoded [Backup]: in its [Backup.backupProfiles], or at its top level in a backup made before
 * profiles. Each field holds one entry, as the encoding of a repeated message gives.
 */
internal object BackupLayout {
    val PROFILE_TAG = lengthDelimitedTag(200)

    // Backup.backupManga, Backup.backupAnime and Backup.backupEntries
    val TOP_LEVEL_ENTRY_TAGS = mapOf(
        lengthDelimitedTag(1) to BackupEntryFormat.LEGACY_MANGA,
        lengthDelimitedTag(202) to BackupEntryFormat.LEGACY_ANIME,
        lengthDelimitedTag(203) to BackupEntryFormat.ENTRY,
    )

    // ProfileScopedBackup.manga, ProfileScopedBackup.anime and ProfileScopedBackup.entries
    val PROFILE_ENTRY_TAGS = mapOf(
        lengthDelimitedTag(3) to BackupEntryFormat.LEGACY_MANGA,
        lengthDelimitedTag(6) to BackupEntryFormat.LEGACY_ANIME,
        lengthDelimitedTag(7) to BackupEntryFormat.ENTRY,
    )

    val TOP_LEVEL_ENTRY_TAG = lengthDelimitedTag(203)

    /**
     * Top-level fields only Katari writes: profiles, the active profile, entries and when the backup was made. Every
     * released Katari backup has profiles or entries; no other app's backup has any of these.
     */
    val KATARI_FIELD_NUMBERS = setOf(200, 201, 203, 205)

    /** [ProfileScopedBackup.entries] */
    val PROFILE_ENTRY_TAG = lengthDelimitedTag(7)
}
