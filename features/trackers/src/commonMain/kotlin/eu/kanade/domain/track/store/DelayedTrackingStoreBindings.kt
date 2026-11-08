package eu.kanade.domain.track.store

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import tachiyomi.core.common.preference.PlatformPreferenceFile
import tachiyomi.core.common.preference.PlatformPreferenceFiles

@ContributesTo(AppScope::class)
@BindingContainer
object DelayedTrackingStoreBindings {
    @Provides
    @SingleIn(AppScope::class)
    fun delayedTrackingStore(files: PlatformPreferenceFiles): DelayedTrackingStore =
        DelayedTrackingStore(files.open(PlatformPreferenceFile.TRACKING_QUEUE))
}
