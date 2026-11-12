package app.katari.android.sync

import android.app.ActivityManager
import android.app.Application
import android.content.Intent
import android.os.PowerManager
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.workDataOf
import app.katari.sync.client.trigger.SyncBackgroundWork
import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.util.system.enqueueUniquePeriodicWorkFromCurrentClock
import eu.kanade.tachiyomi.util.system.setBackoffCriteria
import eu.kanade.tachiyomi.util.system.workManager
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.work.WorkBackoff
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Sync's background work as WorkManager jobs, which also run while the app is closed. The hand-off is ordinary
 * one-time work: expedited work would need a foreground notification below Android 12, for a run of a few seconds. A
 * long run goes in the foreground with a progress notification, since it can take many minutes.
 */
@Inject
class AndroidSyncBackgroundWork(private val application: Application) : SyncBackgroundWork {
    override fun keepConnected(connected: Boolean) {
        val intent = Intent(application, InstantSyncService::class.java)
        if (!connected) {
            application.stopService(intent)
            return
        }
        try {
            ContextCompat.startForegroundService(application, intent)
        } catch (e: IllegalStateException) {
            // Android 12 and later refuse it from the background unless battery optimisation is off; the app's next
            // opening starts it.
            logcat(LogPriority.WARN, e) { "Instant sync not started" }
        }
    }

    override suspend fun schedulePeriodic(enabled: Boolean, every: Duration) {
        if (!enabled) {
            application.workManager.cancelUniqueWork(PERIODIC)
            return
        }
        val request = PeriodicWorkRequestBuilder<SyncJob>(every.inWholeMinutes, TimeUnit.MINUTES)
            .setConstraints(Constraints(requiredNetworkType = NetworkType.CONNECTED))
            .setInputData(workDataOf(SyncJob.KIND to SyncJob.PERIODIC))
            .addTag(PERIODIC)
            .build()
        application.workManager.enqueueUniquePeriodicWorkFromCurrentClock(
            PERIODIC,
            // Keeps the schedule's start, taking a new interval
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    override fun handOff() {
        val request = OneTimeWorkRequestBuilder<SyncJob>()
            .setConstraints(Constraints(requiredNetworkType = NetworkType.CONNECTED))
            .setInputData(workDataOf(SyncJob.KIND to SyncJob.HAND_OFF))
            .setBackoffCriteria(HAND_OFF_BACKOFF)
            .addTag(HAND_OFF)
            .build()
        // A newer hand-off sends everything an older one would have.
        application.workManager.enqueueUniqueWork(HAND_OFF, ExistingWorkPolicy.REPLACE, request)
    }

    override fun retry() {
        val request = OneTimeWorkRequestBuilder<SyncJob>()
            .setConstraints(Constraints(requiredNetworkType = NetworkType.CONNECTED))
            .setInputData(workDataOf(SyncJob.KIND to SyncJob.RETRY))
            .setBackoffCriteria(RETRY_BACKOFF)
            .addTag(RETRY)
            .build()
        // Replacing the work would start it over without its backoff, at once after each run that leaves work.
        application.workManager.enqueueUniqueWork(RETRY, ExistingWorkPolicy.KEEP, request)
    }

    override fun longRun(onMetered: Boolean) {
        val request = OneTimeWorkRequestBuilder<SyncJob>()
            .setConstraints(
                Constraints(requiredNetworkType = if (onMetered) NetworkType.CONNECTED else NetworkType.UNMETERED),
            )
            .setInputData(workDataOf(SyncJob.KIND to SyncJob.LONG_RUN))
            .setBackoffCriteria(LONG_RUN_BACKOFF)
            .addTag(LONG_RUN)
            .build()
        // The work already going carries the run on; a second one would only wait for the same run.
        application.workManager.enqueueUniqueWork(LONG_RUN, ExistingWorkPolicy.KEEP, request)
    }

    override fun dropLongRun() {
        application.workManager.cancelUniqueWork(LONG_RUN)
    }

    override fun allowsBackgroundSync(): Boolean {
        val powerSaving = application.getSystemService<PowerManager>()?.isPowerSaveMode == true
        val restricted = application.getSystemService<ActivityManager>()?.isBackgroundRestricted == true
        return !powerSaving && !restricted
    }

    private companion object {
        const val PERIODIC = "SyncPeriodic"
        const val HAND_OFF = "SyncHandOff"
        val HAND_OFF_BACKOFF = WorkBackoff(initialDelay = 30.seconds)
        const val RETRY = "SyncRetry"
        val RETRY_BACKOFF = WorkBackoff(initialDelay = 30.seconds)
        const val LONG_RUN = "SyncLongRun"
        val LONG_RUN_BACKOFF = WorkBackoff(initialDelay = 30.seconds)
    }
}
