package mihon.desktop.di

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.IntoSet
import mihon.desktop.tracking.InProcessDelayedTrackingUpdateScheduler
import mihon.desktop.work.ResumableWork
import tachiyomi.domain.track.service.DelayedTrackingUpdateScheduler

@BindingContainer
interface TrackerBindings {
    @Binds
    val InProcessDelayedTrackingUpdateScheduler.delayedTrackingUpdateScheduler: DelayedTrackingUpdateScheduler

    @Binds
    @IntoSet
    val InProcessDelayedTrackingUpdateScheduler.resumableDelayedTrackingUpdates: ResumableWork
}
