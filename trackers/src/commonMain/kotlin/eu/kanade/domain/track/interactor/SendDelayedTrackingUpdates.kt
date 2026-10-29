package eu.kanade.domain.track.interactor

import dev.zacsweers.metro.Inject
import eu.kanade.domain.track.store.DelayedTrackingStore
import logcat.LogPriority
import mihon.entry.interactions.tracking.EntryTrackingFeature
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.entry.interactor.GetEntry
import tachiyomi.domain.track.interactor.GetTracks
import tachiyomi.domain.work.WorkBackoff
import tachiyomi.domain.work.WorkOutcome
import kotlin.time.Duration.Companion.minutes

/**
 * Sends the tracking progress kept in [DelayedTrackingStore] because it couldn't reach its tracker. Progress that still
 * doesn't get through stays queued and the pass asks to be retried with [BACKOFF], giving up after [MAX_RETRIES]
 * retries.
 */
@Inject
class SendDelayedTrackingUpdates(
    private val getTracks: GetTracks,
    private val getEntry: GetEntry,
    private val trackingFeature: EntryTrackingFeature,
    private val delayedTrackingStore: DelayedTrackingStore,
) {
    /** @param attempt how many times the pass ran before, 0 on its first run. */
    suspend fun await(attempt: Int): WorkOutcome {
        if (attempt > MAX_RETRIES) return WorkOutcome.Failed

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

        return if (delayedTrackingStore.getItems().isEmpty()) WorkOutcome.Done else WorkOutcome.RetryLater
    }

    companion object {
        const val MAX_RETRIES = 3

        val BACKOFF = WorkBackoff(initialDelay = 5.minutes)
    }
}
