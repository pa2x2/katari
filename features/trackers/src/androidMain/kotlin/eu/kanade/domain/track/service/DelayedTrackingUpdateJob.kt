package eu.kanade.domain.track.service

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dev.zacsweers.metro.Inject
import eu.kanade.domain.track.interactor.SendDelayedTrackingUpdates
import mihon.core.metro.metroGraph
import tachiyomi.domain.work.WorkOutcome

/** Runs [SendDelayedTrackingUpdates] for WorkManager once the device is online. */
class DelayedTrackingUpdateJob(private val context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {

    @Inject
    private lateinit var sendDelayedTrackingUpdates: SendDelayedTrackingUpdates

    override suspend fun doWork(): Result {
        context.metroGraph<DelayedTrackingGraph>().inject(this)
        return when (sendDelayedTrackingUpdates.await(runAttemptCount)) {
            WorkOutcome.Done -> Result.success()
            WorkOutcome.RetryLater -> Result.retry()
            WorkOutcome.Failed -> Result.failure()
        }
    }
}
