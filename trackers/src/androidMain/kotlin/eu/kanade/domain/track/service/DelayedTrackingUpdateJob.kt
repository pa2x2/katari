package eu.kanade.domain.track.service

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dev.zacsweers.metro.Inject
import eu.kanade.domain.track.store.DelayedTrackingStore
import logcat.LogPriority
import mihon.core.metro.metroGraph
import mihon.entry.interactions.tracking.EntryTrackingFeature
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.entry.interactor.GetEntry
import tachiyomi.domain.track.interactor.GetTracks

class DelayedTrackingUpdateJob(private val context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {

    @Inject
    private lateinit var getTracks: GetTracks

    @Inject
    private lateinit var getEntry: GetEntry

    @Inject
    private lateinit var trackingFeature: EntryTrackingFeature

    @Inject
    private lateinit var delayedTrackingStore: DelayedTrackingStore

    override suspend fun doWork(): Result {
        if (runAttemptCount > 3) {
            return Result.failure()
        }

        context.metroGraph<DelayedTrackingGraph>().inject(this)

        withIOContext {
            delayedTrackingStore.getItems()
                .mapNotNull {
                    val track = getTracks.awaitOne(it.trackId)
                    if (track == null) {
                        delayedTrackingStore.remove(it.trackId)
                    }
                    track?.copy(progress = it.progress.toDouble())
                }
                .forEach { track ->
                    logcat(LogPriority.DEBUG) {
                        "Updating delayed track item: ${track.entryId}, progress: ${track.progress}"
                    }
                    val entry = getEntry.await(track.entryId) ?: return@forEach
                    trackingFeature.synchronizeProgress(entry, track.progress, scheduleRetry = false)
                }
        }

        return if (delayedTrackingStore.getItems().isEmpty()) Result.success() else Result.retry()
    }
}
