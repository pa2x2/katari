package mihon.desktop.tracking

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import eu.kanade.domain.track.interactor.SendDelayedTrackingUpdates
import mihon.desktop.work.ExistingWorkPolicy
import mihon.desktop.work.InProcessWork
import mihon.desktop.work.InProcessWorkRequest
import mihon.desktop.work.ResumableWork
import tachiyomi.domain.track.service.DelayedTrackingUpdateScheduler

/** Sends the tracking progress that couldn't reach its tracker as [InProcessWork], once online. */
@Inject
@SingleIn(AppScope::class)
class InProcessDelayedTrackingUpdateScheduler(
    private val work: InProcessWork,
    // Sending progress goes through the tracking that schedules this when progress can't be sent.
    private val sendDelayedTrackingUpdates: () -> SendDelayedTrackingUpdates,
) : DelayedTrackingUpdateScheduler, ResumableWork {
    override val kinds: Set<String> = setOf(NAME)

    override fun setupTask() {
        work.enqueue(
            NAME,
            ExistingWorkPolicy.Replace,
            InProcessWorkRequest(
                resumeAs = NAME,
                requiresNetwork = true,
                backoff = SendDelayedTrackingUpdates.BACKOFF,
            ) { run -> sendDelayedTrackingUpdates().await(run.attempt) },
        )
    }

    override suspend fun resume(kind: String, input: Map<String, String>) {
        setupTask()
    }

    private companion object {
        const val NAME = "DelayedTrackingUpdate"
    }
}
