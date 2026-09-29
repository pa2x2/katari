package mihon.entry.interactions.translate.work

import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import logcat.LogPriority
import mihon.entry.interactions.translate.EntryTranslateRuntimeAvailability
import tachiyomi.core.common.util.system.logcat
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/**
 * Drains the translation queue in the background, with an ongoing notification of the chapter being translated. While
 * the charging or Wi-Fi rule holds the queue, it waits in the foreground and says what it waits for.
 */
class EntryTranslateJob(context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams) {

    override suspend fun getForegroundInfo(): ForegroundInfo = foregroundInfo(dependencies().notifier)

    override suspend fun doWork(): Result {
        val dependencies = dependencies()
        while (true) {
            if (!dependencies.runner.hasPendingWork()) return Result.success()
            try {
                setForeground(foregroundInfo(dependencies.notifier))
            } catch (error: IllegalStateException) {
                logcat(LogPriority.ERROR, error) { "Not allowed to foreground translation worker" }
            }
            if (runUntilHeld(dependencies)) return Result.success()
        }
    }

    /** Runs until the queue is idle, which returns true, or until the rules hold it, which returns false. */
    private suspend fun runUntilHeld(dependencies: Dependencies): Boolean = coroutineScope {
        val (runner, notifier, conditions) = dependencies
        val notifications = launch {
            combine(runner.active, conditions.waiting, ::Pair).collect { (active, waiting) ->
                notifier.update(active, waiting)
            }
        }
        // Chapters cancelled while the queue is held leave nothing to wait for.
        combine(conditions.waiting, runner.pending) { waiting, pending -> waiting == null || !pending }.first { it }
        val processing = async { runner.runUntilIdle() }
        val held = async { conditions.waiting.first { it != null } }
        select {
            processing.onAwait {
                held.cancelAndJoin()
                true
            }
            held.onAwait {
                // The chapter being translated keeps its place in the queue and starts over when the rules allow.
                processing.cancelAndJoin()
                false
            }
        }.also { notifications.cancel() }
    }

    private fun foregroundInfo(notifier: EntryTranslateNotifier) = ForegroundInfo(
        notifier.notificationId,
        notifier.notification(null, null),
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC else 0,
    )

    private suspend fun dependencies(): Dependencies {
        EntryTranslateRuntimeAvailability.awaitInstalled()
        return Dependencies(runner = Injekt.get(), notifier = Injekt.get(), conditions = Injekt.get())
    }

    private data class Dependencies(
        val runner: EntryTranslateQueueRunner,
        val notifier: EntryTranslateNotifier,
        val conditions: EntryTranslateConditions,
    )
}
