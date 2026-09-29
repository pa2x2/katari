package mihon.entry.interactions.translate

import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/** Drains the translation queue in the background, with an ongoing notification of the chapter being translated. */
class EntryTranslateJob(context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams) {

    override suspend fun getForegroundInfo(): ForegroundInfo = foregroundInfo(dependencies().notifier)

    override suspend fun doWork(): Result {
        val (runner, notifier) = dependencies()
        if (!runner.hasPendingWork()) return Result.success()
        try {
            setForeground(foregroundInfo(notifier))
        } catch (error: IllegalStateException) {
            logcat(LogPriority.ERROR, error) { "Not allowed to foreground translation worker" }
        }
        coroutineScope {
            val progress = launch { runner.active.collect(notifier::update) }
            runner.runUntilIdle()
            progress.cancel()
        }
        return Result.success()
    }

    private fun foregroundInfo(notifier: EntryTranslateNotifier) = ForegroundInfo(
        notifier.notificationId,
        notifier.notification(null),
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC else 0,
    )

    private suspend fun dependencies(): Dependencies {
        EntryTranslateRuntimeAvailability.awaitInstalled()
        return Dependencies(runner = Injekt.get(), notifier = Injekt.get())
    }

    private data class Dependencies(
        val runner: EntryTranslateQueueRunner,
        val notifier: EntryTranslateNotifier,
    )
}
