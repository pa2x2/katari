package eu.kanade.tachiyomi.data.backup

import eu.kanade.tachiyomi.data.backup.create.BackupCreateJob
import eu.kanade.tachiyomi.data.backup.restore.BackupRestoreJob

/** Graph entry points of the backup jobs, which WorkManager constructs. */
interface BackupGraph {
    fun inject(job: BackupCreateJob)

    fun inject(job: BackupRestoreJob)
}
