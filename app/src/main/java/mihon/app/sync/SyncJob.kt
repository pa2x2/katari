package mihon.app.sync

import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import app.katari.sync.client.trigger.SyncBackgroundRun
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.util.system.cancelNotification
import eu.kanade.tachiyomi.util.system.notify
import eu.kanade.tachiyomi.util.system.setForegroundSafely
import kotlinx.coroutines.flow.first
import mihon.app.di.appGraph
import tachiyomi.domain.work.WorkOutcome

/**
 * Runs [SyncBackgroundRun] for WorkManager, as the periodic sync, the hand-off or the long run its input names. A long
 * run goes in the foreground, showing its progress as it goes (D007).
 */
class SyncJob(context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams) {

    private val graph = context.appGraph
    private val run = graph.syncBackgroundRun
    private val notification = graph.syncLongRunNotification

    override suspend fun doWork(): Result {
        val outcome = when (inputData.getString(KIND)) {
            HAND_OFF -> run.handOff()
            LONG_RUN -> {
                setForegroundSafely()
                try {
                    run.longRun { applicationContext.notify(Notifications.ID_SYNC_LONG_RUN, notification.build(it)) }
                } finally {
                    applicationContext.cancelNotification(Notifications.ID_SYNC_LONG_RUN)
                }
            }
            else -> run.periodic()
        }
        return when (outcome) {
            WorkOutcome.Done -> Result.success()
            WorkOutcome.RetryLater -> Result.retry()
            WorkOutcome.Failed -> Result.failure()
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo = ForegroundInfo(
        Notifications.ID_SYNC_LONG_RUN,
        notification.build(graph.syncStatusFlow.status().first()),
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC else 0,
    )

    companion object {
        const val KIND = "kind"
        const val PERIODIC = "periodic"
        const val HAND_OFF = "handOff"
        const val LONG_RUN = "longRun"
    }
}
