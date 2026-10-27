package tachiyomi.domain.backup.service

import tachiyomi.domain.backup.model.BackupOptions

interface BackupCreateSchedule {
    /**
     * Schedules automatic backups at the backup interval, or cancels them when it is 0.
     *
     * @param prefInterval the interval when its preference is about to change and doesn't hold it yet.
     */
    suspend fun setupTask(prefInterval: Int? = null)

    /** Backs up into the document at [uri] now, unless a backup the user started still runs. */
    fun startNow(uri: String, options: BackupOptions)

    suspend fun isManualBackupRunning(): Boolean
}
