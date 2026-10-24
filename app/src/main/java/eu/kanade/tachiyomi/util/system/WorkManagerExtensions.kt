package eu.kanade.tachiyomi.util.system

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.guava.await
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds

val Context.workManager: WorkManager
    get() = WorkManager.getInstance(this)

suspend fun WorkManager.isRunning(tag: String): Boolean {
    val list = getWorkInfosByTag(tag).await()
    return list.any { it.state == WorkInfo.State.RUNNING }
}

/**
 * [WorkManager.enqueueUniquePeriodicWork], except that a schedule the clock left behind starts over from now.
 *
 * WorkManager counts each period from the start of the previous run. Once the clock is set back past that start, the
 * next run lies more than a period ahead and stays there until the clock catches up, which can take months. Every
 * [policy] keeps that start, so such a schedule is replaced instead.
 */
suspend fun WorkManager.enqueueUniquePeriodicWorkFromCurrentClock(
    uniqueWorkName: String,
    policy: ExistingPeriodicWorkPolicy,
    request: PeriodicWorkRequest,
) {
    val now = Clock.System.now().toEpochMilliseconds()
    val clockWentBack = getWorkInfosForUniqueWork(uniqueWorkName).await().any { it.isScheduledBeyondItsPeriod(now) }
    enqueueUniquePeriodicWork(
        uniqueWorkName,
        if (clockWentBack) ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE else policy,
        request,
    )
}

/**
 * Whether this periodic work waits longer than any schedule WorkManager makes on a steady clock: its initial delay plus
 * a whole period, or plus the longest backoff after a retry.
 */
internal fun WorkInfo.isScheduledBeyondItsPeriod(nowMillis: Long): Boolean {
    val period = periodicityInfo?.repeatIntervalMillis ?: return false
    if (state != WorkInfo.State.ENQUEUED || nextScheduleTimeMillis == Long.MAX_VALUE) return false
    return nextScheduleTimeMillis - nowMillis > initialDelayMillis + maxOf(period, WorkRequest.MAX_BACKOFF_MILLIS)
}

/**
 * Makes this worker run in the context of a foreground service.
 *
 * Note that this function is a no-op if the process is subject to foreground
 * service restrictions.
 *
 * Moving to foreground service context requires the worker to run a bit longer,
 * allowing Service.startForeground() to be called and avoiding system crash.
 */
suspend fun CoroutineWorker.setForegroundSafely() {
    try {
        setForeground(getForegroundInfo())
        delay(0.5.seconds)
    } catch (e: IllegalStateException) {
        logcat(LogPriority.ERROR, e) { "Not allowed to set foreground job" }
    }
}
