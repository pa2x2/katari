package tachiyomi.domain.backup.service

import tachiyomi.domain.backup.model.RestoreOptions

interface BackupRestoreRunner {
    suspend fun isRunning(): Boolean

    /** Restores the backup document at [uri], unless a restore already runs. */
    fun start(uri: String, options: RestoreOptions, sync: Boolean = false)

    fun stop()
}
