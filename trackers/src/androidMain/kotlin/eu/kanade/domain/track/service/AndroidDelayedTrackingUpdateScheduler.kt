package eu.kanade.domain.track.service

import android.app.Application
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import dev.zacsweers.metro.Inject
import eu.kanade.domain.track.interactor.SendDelayedTrackingUpdates
import tachiyomi.domain.track.service.DelayedTrackingUpdateScheduler
import java.util.concurrent.TimeUnit

@Inject
class AndroidDelayedTrackingUpdateScheduler(private val application: Application) : DelayedTrackingUpdateScheduler {
    override fun setupTask() {
        val constraints = Constraints(
            requiredNetworkType = NetworkType.CONNECTED,
        )

        val backoff = SendDelayedTrackingUpdates.BACKOFF
        val request = OneTimeWorkRequestBuilder<DelayedTrackingUpdateJob>()
            .setConstraints(constraints)
            .setBackoffCriteria(
                if (backoff.exponential) BackoffPolicy.EXPONENTIAL else BackoffPolicy.LINEAR,
                backoff.initialDelay.inWholeMilliseconds,
                TimeUnit.MILLISECONDS,
            )
            .addTag(TAG)
            .build()

        WorkManager.getInstance(application).enqueueUniqueWork(TAG, ExistingWorkPolicy.REPLACE, request)
    }

    private companion object {
        const val TAG = "DelayedTrackingUpdate"
    }
}
