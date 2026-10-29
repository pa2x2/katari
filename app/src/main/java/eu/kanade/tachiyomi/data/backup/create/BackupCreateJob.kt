package eu.kanade.tachiyomi.data.backup.create

import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.net.toUri
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.hippo.unifile.UniFile
import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.data.backup.BackupNotifier
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.util.system.cancelNotification
import eu.kanade.tachiyomi.util.system.setForegroundSafely
import logcat.LogPriority
import mihon.app.di.appGraph
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.backup.model.BackupOptions
import tachiyomi.domain.backup.service.BackupRestoreRunner
import tachiyomi.domain.storage.DocumentUri

class BackupCreateJob(private val context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {

    @Inject
    private lateinit var notifier: BackupNotifier

    @Inject
    private lateinit var backupCreatorFactory: BackupCreator.Factory

    @Inject
    private lateinit var restoreRunner: BackupRestoreRunner

    @Inject
    private lateinit var automaticBackupLocation: AutomaticBackupLocation

    override suspend fun doWork(): Result {
        context.appGraph.inject(this)

        val isAutoBackup = inputData.getBoolean(IS_AUTO_BACKUP_KEY, true)

        if (isAutoBackup && restoreRunner.isRunning()) return Result.retry()

        // Automatic backups go into a new file in their folder, the user's own into the document they picked.
        val backupFile: () -> DocumentUri
        if (isAutoBackup) {
            val folder = automaticBackupLocation.folder() ?: return Result.failure()
            backupFile = { automaticBackupLocation.next(folder) }
        } else {
            val location = inputData.getString(LOCATION_URI_KEY)?.let(::DocumentUri) ?: return Result.failure()
            backupFile = { location }
        }

        setForegroundSafely()

        val options = inputData.getBooleanArray(OPTIONS_KEY)
            ?.let { BackupOptions.fromBooleanArray(it, inputData.getLongArray(PROFILES_KEY)?.toSet()) }
            ?: BackupOptions()

        return try {
            val location = backupCreatorFactory.create(isAutoBackup, notifier::showBackupProgress)
                .backup(backupFile(), options)
            if (!isAutoBackup) {
                notifier.showBackupComplete(UniFile.fromUri(context, location.value.toUri())!!)
            }
            Result.success()
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            if (!isAutoBackup) notifier.showBackupError(e.message)
            Result.failure()
        } finally {
            context.cancelNotification(Notifications.ID_BACKUP_PROGRESS)
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        return ForegroundInfo(
            Notifications.ID_BACKUP_PROGRESS,
            notifier.backupProgressNotification(BackupCreationProgress.Preparing),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            } else {
                0
            },
        )
    }

    companion object {
        internal const val IS_AUTO_BACKUP_KEY = "is_auto_backup" // Boolean
        internal const val LOCATION_URI_KEY = "location_uri" // String
        internal const val OPTIONS_KEY = "options" // BooleanArray
        internal const val PROFILES_KEY = "profiles" // LongArray, absent to include every profile
    }
}
