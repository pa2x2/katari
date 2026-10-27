package eu.kanade.tachiyomi.data.backup.create

import android.app.Application
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.workDataOf
import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.util.system.enqueueUniquePeriodicWorkFromCurrentClock
import eu.kanade.tachiyomi.util.system.isRunning
import eu.kanade.tachiyomi.util.system.workManager
import tachiyomi.domain.backup.model.BackupOptions
import tachiyomi.domain.backup.service.BackupCreateSchedule
import tachiyomi.domain.backup.service.BackupPreferences
import java.util.concurrent.TimeUnit

@Inject
class AndroidBackupCreateSchedule(
    private val application: Application,
    private val backupPreferences: BackupPreferences,
) : BackupCreateSchedule {
    override suspend fun isManualBackupRunning(): Boolean {
        return application.workManager.isRunning(TAG_MANUAL)
    }

    override suspend fun setupTask(prefInterval: Int?) {
        val interval = prefInterval ?: backupPreferences.backupInterval.get()
        if (interval > 0) {
            val constraints = Constraints(
                requiresBatteryNotLow = true,
            )

            val request = PeriodicWorkRequestBuilder<BackupCreateJob>(
                interval.toLong(),
                TimeUnit.HOURS,
                10,
                TimeUnit.MINUTES,
            )
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.MINUTES)
                .addTag(TAG_AUTO)
                .setConstraints(constraints)
                .setInputData(workDataOf(BackupCreateJob.IS_AUTO_BACKUP_KEY to true))
                .build()

            application.workManager.enqueueUniquePeriodicWorkFromCurrentClock(
                TAG_AUTO,
                ExistingPeriodicWorkPolicy.UPDATE,
                request,
            )
        } else {
            application.workManager.cancelUniqueWork(TAG_AUTO)
        }
    }

    override fun startNow(uri: String, options: BackupOptions) {
        val inputData = workDataOf(
            BackupCreateJob.IS_AUTO_BACKUP_KEY to false,
            BackupCreateJob.LOCATION_URI_KEY to uri,
            BackupCreateJob.OPTIONS_KEY to options.asBooleanArray(),
            BackupCreateJob.PROFILES_KEY to options.profileIds?.toLongArray(),
        )
        val request = OneTimeWorkRequestBuilder<BackupCreateJob>()
            .addTag(TAG_MANUAL)
            .setInputData(inputData)
            .build()
        application.workManager.enqueueUniqueWork(TAG_MANUAL, ExistingWorkPolicy.KEEP, request)
    }

    private companion object {
        const val TAG_AUTO = "BackupCreator"
        const val TAG_MANUAL = "$TAG_AUTO:manual"
    }
}
