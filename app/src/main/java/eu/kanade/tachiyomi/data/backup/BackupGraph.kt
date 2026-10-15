package eu.kanade.tachiyomi.data.backup

import eu.kanade.tachiyomi.data.backup.create.BackupCreateJob
import eu.kanade.tachiyomi.data.backup.restore.BackupRestoreJob
import tachiyomi.domain.backup.service.BackupPreferences

/** Graph entry points of the backup jobs, which WorkManager constructs. */
interface BackupGraph {
    fun inject(job: BackupCreateJob)

    fun inject(job: BackupRestoreJob)

    val backupPreferences: BackupPreferences
}
