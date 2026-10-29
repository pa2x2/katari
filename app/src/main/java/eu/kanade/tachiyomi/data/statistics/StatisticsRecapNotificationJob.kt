package eu.kanade.tachiyomi.data.statistics

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import eu.kanade.tachiyomi.ui.stats.recap.delivery.StatisticsRecapDue
import logcat.LogPriority
import mihon.app.di.appGraph
import tachiyomi.core.common.util.system.logcat

/** Posts the active profile's [StatisticsRecapDue] recaps, checked daily. */
class StatisticsRecapNotificationJob(context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {

    private val graph = context.appGraph

    private val notifier = StatisticsRecapNotifier(context)

    override suspend fun doWork(): Result {
        return try {
            graph.statisticsRecapDue.deliver(graph.activeProfileProvider.activeProfileId, notifier::post)
            Result.success()
        } catch (error: Exception) {
            logcat(LogPriority.ERROR, error)
            Result.retry()
        }
    }
}
