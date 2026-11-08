package tachiyomi.domain.backup.service

import app.katari.core.common.platform.PlatformFacility
import tachiyomi.domain.backup.model.BackupOptions
import tachiyomi.domain.storage.DocumentUri

@PlatformFacility
interface BackupCreateSchedule {
    /** False where the platform doesn't back up on its own; the backup settings then hide the automatic backup rows. */
    val isAutomaticBackupOffered: Boolean

    /**
     * Schedules automatic backups at the backup interval, or cancels them when it is 0.
     *
     * @param prefInterval the interval when its preference is about to change and doesn't hold it yet.
     */
    suspend fun setupTask(prefInterval: Int? = null)

    /** Backs up into the document at [uri] now, unless a backup the user started still runs. */
    fun startNow(uri: DocumentUri, options: BackupOptions)

    suspend fun isManualBackupRunning(): Boolean
}
