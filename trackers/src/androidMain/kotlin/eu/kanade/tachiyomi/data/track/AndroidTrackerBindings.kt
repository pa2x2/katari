package eu.kanade.tachiyomi.data.track

import android.app.Application
import android.content.Context
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import eu.kanade.domain.track.service.AndroidDelayedTrackingUpdateScheduler
import eu.kanade.domain.track.store.DelayedTrackingStore
import eu.kanade.tachiyomi.data.track.oauth.ExternalLoginRedirect
import eu.kanade.tachiyomi.data.track.oauth.TrackLoginActivityRedirect
import eu.kanade.tachiyomi.data.track.source.SourceSettingsTrackerSourceAccess
import eu.kanade.tachiyomi.data.track.source.TrackerSourceAccess
import tachiyomi.core.common.preference.AndroidPreferenceStore
import tachiyomi.domain.track.service.DelayedTrackingUpdateScheduler

@ContributesTo(AppScope::class)
@BindingContainer
interface AndroidTrackerBindings {
    @Binds
    val SourceSettingsTrackerSourceAccess.trackerSourceAccess: TrackerSourceAccess

    @Binds
    val TrackLoginActivityRedirect.externalLoginRedirect: ExternalLoginRedirect

    @Binds
    val ToastTrackerFailureMessages.trackerFailureMessages: TrackerFailureMessages

    @Binds
    val AndroidDelayedTrackingUpdateScheduler.delayedTrackingUpdateScheduler: DelayedTrackingUpdateScheduler

    companion object {
        @Provides
        @SingleIn(AppScope::class)
        fun delayedTrackingStore(application: Application): DelayedTrackingStore = DelayedTrackingStore(
            AndroidPreferenceStore(
                application,
                application.getSharedPreferences("tracking_queue", Context.MODE_PRIVATE),
            ),
        )
    }
}
