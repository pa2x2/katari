package mihon.entry.interactions.download

import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import logcat.LogPriority
import mihon.core.metro.metroGraph
import tachiyomi.core.common.util.system.logcat

/** Runs [EntryDownloadRun] as a foreground worker, with the download progress as its notification. */
class EntryDownloadJob(context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams) {
    private val dependencies by lazy {
        applicationContext.metroGraph<EntryDownloadWorkerGraph>().entryDownloadWorkerDependencies
    }

    override suspend fun getForegroundInfo(): ForegroundInfo = dependencies.notificationProvider.foregroundInfo()

    override suspend fun doWork(): Result {
        dependencies.run.run(onPending = { setForegroundSafely(dependencies.notificationProvider.foregroundInfo()) })
        return Result.success()
    }
}

private fun EntryDownloadForegroundNotificationProvider.foregroundInfo() = ForegroundInfo(
    notificationId,
    notification(),
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC else 0,
)

private suspend fun CoroutineWorker.setForegroundSafely(info: ForegroundInfo) {
    try {
        setForeground(info)
    } catch (error: IllegalStateException) {
        logcat(LogPriority.ERROR, error) { "Not allowed to foreground download worker" }
    }
}
