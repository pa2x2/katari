package eu.kanade.tachiyomi.data.backup.create

sealed interface BackupCreationProgress {
    data object Preparing : BackupCreationProgress

    data class Entries(val backedUp: Int, val total: Int) : BackupCreationProgress

    /** Encoding, writing and validating the file, which has no measurable steps. */
    data object Saving : BackupCreationProgress
}
