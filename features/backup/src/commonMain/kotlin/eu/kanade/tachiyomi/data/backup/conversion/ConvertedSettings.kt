package eu.kanade.tachiyomi.data.backup.conversion

import eu.kanade.tachiyomi.data.backup.conversion.ConversionReport.Item
import eu.kanade.tachiyomi.data.backup.models.BackupPreference

/** A backup's app settings as Katari's, with what the report lists of them. */
internal class ConvertedSettings(
    val preferences: List<BackupPreference>,
    val carried: List<Item>,
    val leftBehind: List<Item>,
)
