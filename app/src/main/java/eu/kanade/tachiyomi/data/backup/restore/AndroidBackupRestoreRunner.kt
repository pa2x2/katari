package eu.kanade.tachiyomi.data.backup.restore

import android.app.Application
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.workDataOf
import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.util.system.isRunning
import eu.kanade.tachiyomi.util.system.workManager
import tachiyomi.domain.backup.model.RestoreOptions
import tachiyomi.domain.backup.service.BackupRestoreRunner

@Inject
class AndroidBackupRestoreRunner(private val application: Application) : BackupRestoreRunner {
    override suspend fun isRunning(): Boolean {
        return application.workManager.isRunning(TAG)
    }

    override fun start(uri: String, options: RestoreOptions, sync: Boolean) {
        val inputData = workDataOf(
            BackupRestoreJob.LOCATION_URI_KEY to uri,
            BackupRestoreJob.SYNC_KEY to sync,
            BackupRestoreJob.OPTIONS_KEY to options.asBooleanArray(),
            BackupRestoreJob.PROFILES_KEY to options.profileUuids?.toTypedArray(),
        )
        val request = OneTimeWorkRequestBuilder<BackupRestoreJob>()
            .addTag(TAG)
            .setInputData(inputData)
            .build()
        application.workManager.enqueueUniqueWork(TAG, ExistingWorkPolicy.KEEP, request)
    }

    override fun stop() {
        application.workManager.cancelUniqueWork(TAG)
    }

    private companion object {
        const val TAG = "BackupRestore"
    }
}
