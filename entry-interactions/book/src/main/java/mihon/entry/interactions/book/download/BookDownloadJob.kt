package mihon.entry.interactions.book.download

import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import logcat.LogPriority
import mihon.entry.interactions.EntryDownloadNotifications
import mihon.entry.interactions.entryDownloadNotificationBuilder
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.download.service.DownloadPreferences
import tachiyomi.i18n.MR
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

internal class BookDownloadJob(context: Context, workerParams: WorkerParameters) : CoroutineWorker(
    context,
    workerParams,
) {
    private val manager: BookDownloadManager = Injekt.get()

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val notification = applicationContext.entryDownloadNotificationBuilder(
            EntryDownloadNotifications.CHANNEL_PROGRESS,
        ) {
            setContentTitle(applicationContext.stringResource(MR.strings.download_notifier_downloader_title))
            setSmallIcon(android.R.drawable.stat_sys_download)
            setOngoing(true)
        }.build()
        return ForegroundInfo(
            EntryDownloadNotifications.ID_BOOK_PROGRESS,
            notification,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            } else {
                0
            },
        )
    }

    override suspend fun doWork(): Result {
        try {
            setForeground(getForegroundInfo())
        } catch (error: IllegalStateException) {
            logcat(LogPriority.ERROR, error) { "Not allowed to foreground BOOK downloader" }
        }
        manager.runDownloads()
        return Result.success()
    }

    companion object {
        private const val TAG = "BookDownloader"

        fun start(
            context: Context,
            requireUnmeteredNetwork: Boolean = Injekt.get<DownloadPreferences>().downloadOnlyOverWifi.get(),
        ) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(
                    if (requireUnmeteredNetwork) NetworkType.UNMETERED else NetworkType.CONNECTED,
                )
                .build()
            val request = OneTimeWorkRequestBuilder<BookDownloadJob>()
                .setConstraints(constraints)
                .addTag(TAG)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(TAG, ExistingWorkPolicy.REPLACE, request)
        }

        fun stop(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(TAG)
        }
    }
}
