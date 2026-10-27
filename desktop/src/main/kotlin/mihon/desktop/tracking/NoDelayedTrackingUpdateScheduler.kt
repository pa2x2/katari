package mihon.desktop.tracking

import dev.zacsweers.metro.Inject
import tachiyomi.domain.track.service.DelayedTrackingUpdateScheduler

/**
 * Desktop runs no background work yet, so progress that couldn't reach its tracker stays queued in the
 * `DelayedTrackingStore` and is not retried. Stands in until desktop can run work when it is back online.
 */
@Inject
class NoDelayedTrackingUpdateScheduler : DelayedTrackingUpdateScheduler {
    override fun setupTask() = Unit
}
