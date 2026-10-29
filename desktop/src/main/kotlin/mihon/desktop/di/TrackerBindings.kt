package mihon.desktop.di

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import eu.kanade.domain.track.store.DelayedTrackingStore
import mihon.desktop.storage.DesktopDirectories
import mihon.desktop.tracking.NoDelayedTrackingUpdateScheduler
import tachiyomi.core.common.preference.DesktopPreferenceStore
import tachiyomi.domain.track.service.DelayedTrackingUpdateScheduler

@BindingContainer
interface TrackerBindings {
    @Binds
    val NoDelayedTrackingUpdateScheduler.delayedTrackingUpdateScheduler: DelayedTrackingUpdateScheduler

    companion object {
        // In the data directory, since it holds progress not yet sent anywhere else.
        @Provides
        @SingleIn(AppScope::class)
        fun delayedTrackingStore(directories: DesktopDirectories): DelayedTrackingStore =
            DelayedTrackingStore(DesktopPreferenceStore(directories.data.resolve("tracking-queue.json")))
    }
}
