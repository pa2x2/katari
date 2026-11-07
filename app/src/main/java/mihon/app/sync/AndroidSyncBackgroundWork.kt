package mihon.app.sync

import android.app.ActivityManager
import android.app.Application
import android.os.PowerManager
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
import tachiyomi.domain.work.WorkBackoff
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.seconds

/**
 * Sync's background work as WorkManager jobs, which also run while the app is closed. The hand-off is ordinary
 * one-time work: expedited work would need a foreground notification below Android 12, for a run of a few seconds. The
 * first download runs in the foreground with a progress notification, since it can take many minutes.
 */
@Inject
class AndroidSyncBackgroundWork(private val application: Application) : SyncBackgroundWork {
    override suspend fun schedulePeriodic(enabled: Boolean) {
        if (!enabled) {
            application.workManager.cancelUniqueWork(PERIODIC)
            return
        }
        val request = PeriodicWorkRequestBuilder<SyncJob>(SyncBackgroundWork.PERIOD.inWholeMinutes, TimeUnit.MINUTES)
            .setConstraints(Constraints(requiredNetworkType = NetworkType.CONNECTED))
            .setInputData(workDataOf(SyncJob.KIND to SyncJob.PERIODIC))
            .addTag(PERIODIC)
            .build()
        application.workManager.enqueueUniquePeriodicWorkFromCurrentClock(
            PERIODIC,
            ExistingPeriodicWorkPolicy.KEEP,
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

    override fun firstDownload(onMetered: Boolean) {
        val request = OneTimeWorkRequestBuilder<SyncJob>()
            .setConstraints(
                Constraints(requiredNetworkType = if (onMetered) NetworkType.CONNECTED else NetworkType.UNMETERED),
            )
            .setInputData(workDataOf(SyncJob.KIND to SyncJob.FIRST_DOWNLOAD))
            .setBackoffCriteria(FIRST_DOWNLOAD_BACKOFF)
            .addTag(FIRST_DOWNLOAD)
            .build()
        // The work already going carries the download on; a second one would only wait for the same run.
        application.workManager.enqueueUniqueWork(FIRST_DOWNLOAD, ExistingWorkPolicy.KEEP, request)
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
        const val FIRST_DOWNLOAD = "SyncFirstDownload"
        val FIRST_DOWNLOAD_BACKOFF = WorkBackoff(initialDelay = 30.seconds)
    }
}
