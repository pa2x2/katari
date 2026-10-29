package mihon.desktop.di

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import eu.kanade.domain.track.store.DelayedTrackingStore
import eu.kanade.tachiyomi.data.track.TrackerFailureMessages
import eu.kanade.tachiyomi.data.track.oauth.ExternalLoginRedirect
import eu.kanade.tachiyomi.source.adapter.legacySourceClassName
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import mihon.desktop.storage.DesktopDirectories
import mihon.desktop.tracking.NoDelayedTrackingUpdateScheduler
import mihon.desktop.tracking.NoExternalLoginRedirect
import mihon.desktop.tracking.NoTrackerFailureMessages
import mihon.entry.interactions.host.tracking.LegacySourceClassNames
import tachiyomi.core.common.preference.DesktopPreferenceStore
import tachiyomi.domain.track.service.DelayedTrackingUpdateScheduler

@BindingContainer
interface TrackerBindings {
    @Binds
    val NoDelayedTrackingUpdateScheduler.delayedTrackingUpdateScheduler: DelayedTrackingUpdateScheduler

    @Binds
    val NoExternalLoginRedirect.externalLoginRedirect: ExternalLoginRedirect

    @Binds
    val NoTrackerFailureMessages.trackerFailureMessages: TrackerFailureMessages

    companion object {
        @Provides
        fun legacySourceClassNames(): LegacySourceClassNames = LegacySourceClassNames(
            UnifiedSource::legacySourceClassName,
        )

        // In the data directory, since it holds progress not yet sent anywhere else.
        @Provides
        @SingleIn(AppScope::class)
        fun delayedTrackingStore(directories: DesktopDirectories): DelayedTrackingStore =
            DelayedTrackingStore(DesktopPreferenceStore(directories.data.resolve("tracking-queue.json")))
    }
}
