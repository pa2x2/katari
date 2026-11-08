package tachiyomi.domain.backup.service

import app.katari.core.common.platform.PlatformFacility
import tachiyomi.domain.backup.model.RestoreOptions
import tachiyomi.domain.storage.DocumentUri

@PlatformFacility
interface BackupRestoreRunner {
    suspend fun isRunning(): Boolean

    /** Restores the backup document at [uri], unless a restore already runs. */
    fun start(uri: DocumentUri, options: RestoreOptions, sync: Boolean = false)

    fun stop()
}
