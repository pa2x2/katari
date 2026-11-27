package eu.kanade.tachiyomi.data.backup.conversion

/**
 * An app whose backups [format] reads, by the name the tool lists it under: the family's own app or one of its forks.
 * Converting names the Katari backup after it, and a new profile restored from it takes its name.
 */
data class BackupApp(val name: String, val format: BackupFormat)
