package eu.kanade.tachiyomi.data.backup.stream

import app.katari.profiles.core.ProfileScopedBackup
import eu.kanade.tachiyomi.data.backup.models.Backup

/**
 * Where entries sit in an encoded [Backup]: in its [Backup.backupProfiles], or at its top level in a backup made before
 * profiles. Each field holds one entry, as the encoding of a repeated message gives.
 */
internal object BackupLayout {
    val PROFILE_TAG = lengthDelimitedTag(200)

    /** [Backup.backupEntries] */
    val TOP_LEVEL_ENTRY_TAG = lengthDelimitedTag(203)

    /**
     * Top-level fields only Katari writes: profiles, the active profile, entries, when the backup was made and the app a
     * converted one came from. Every released Katari backup has profiles or entries, and every converted one says where
     * it came from; no other app's backup has any of these.
     */
    val KATARI_FIELD_NUMBERS = setOf(200, 201, 203, 205, 206)

    /** [ProfileScopedBackup.entries] */
    val PROFILE_ENTRY_TAG = lengthDelimitedTag(7)
}
