package mihon.entry.interactions.translate.work

import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import logcat.LogPriority
import mihon.core.metro.metroGraph
import mihon.entry.interactions.translate.notification.EntryTranslateNotifier
import tachiyomi.core.common.util.system.logcat

/**
 * Runs [EntryTranslateRun] as a foreground worker, with an ongoing notification of the chapter being translated or of
 * what the queue waits for. Pausing the queue ends it until the queue is resumed.
 */
class EntryTranslateJob(context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams) {

    private val dependencies by lazy {
        applicationContext.metroGraph<EntryTranslateWorkerGraph>().entryTranslateWorkerDependencies
    }

    override suspend fun getForegroundInfo(): ForegroundInfo = dependencies.notifier.foregroundInfo()

    override suspend fun doWork(): Result {
        dependencies.run.run(
            onPending = {
                try {
                    setForeground(dependencies.notifier.foregroundInfo())
                } catch (error: IllegalStateException) {
                    logcat(LogPriority.ERROR, error) { "Not allowed to foreground translation worker" }
                }
            },
            onStatus = dependencies.notifier::update,
        )
        return Result.success()
    }
}

private fun EntryTranslateNotifier.foregroundInfo() = ForegroundInfo(
    notificationId,
    notification(null, null),
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC else 0,
)
