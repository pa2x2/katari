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
import mihon.entry.interactions.translate.notification.EntryTranslateNotifier
import tachiyomi.core.common.util.system.logcat
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/**
 * Drains the translation queue in the background, with an ongoing notification of the chapter being translated. While
 * the charging or Wi-Fi rule holds the queue, it waits in the foreground and says what it waits for; pausing the queue
 * ends it until the queue is resumed.
 */
class EntryTranslateJob(context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams) {

    override suspend fun getForegroundInfo(): ForegroundInfo = foregroundInfo(dependencies().notifier)

    override suspend fun doWork(): Result {
        val dependencies = dependencies()
        while (true) {
            if (dependencies.conditions.paused.first() || !dependencies.runner.hasPendingWork()) {
                return Result.success()
            }
            try {
                setForeground(foregroundInfo(dependencies.notifier))
            } catch (error: IllegalStateException) {
                logcat(LogPriority.ERROR, error) { "Not allowed to foreground translation worker" }
            }
            if (runUntilHeld(dependencies)) return Result.success()
        }
    }

    /**
     * Runs until the queue is idle or paused, which returns true, or until the rules hold it, which returns false.
     */
    private suspend fun runUntilHeld(dependencies: Dependencies): Boolean = coroutineScope {
        val (runner, notifier, conditions) = dependencies
        val notifications = launch {
            combine(runner.active, conditions.waiting, ::Pair).collect { (active, waiting) ->
                notifier.update(active, waiting)
            }
        }
        // Chapters cancelled or a queue paused while it is held leave nothing to wait for.
        combine(conditions.waiting, runner.pending, conditions.paused) { waiting, pending, paused ->
            waiting == null || !pending || paused
        }.first { it }
        if (conditions.paused.first()) {
            notifications.cancel()
            return@coroutineScope true
        }
        val processing = async { runner.runUntilIdle() }
        val held = async {
            combine(conditions.waiting, conditions.paused) { waiting, paused -> waiting != null || paused }.first { it }
        }
        select {
            processing.onAwait {
                held.cancelAndJoin()
                true
            }
            held.onAwait {
                // The chapter being translated keeps its place in the queue and starts over when it may run again.
                processing.cancelAndJoin()
                conditions.paused.first()
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
