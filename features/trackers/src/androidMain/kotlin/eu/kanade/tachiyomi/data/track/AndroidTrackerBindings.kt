package eu.kanade.tachiyomi.data.track

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.ContributesTo
import eu.kanade.domain.track.service.AndroidDelayedTrackingUpdateScheduler
import eu.kanade.tachiyomi.data.track.oauth.ExternalLoginRedirect
import eu.kanade.tachiyomi.data.track.oauth.TrackLoginActivityRedirect
import tachiyomi.domain.track.service.DelayedTrackingUpdateScheduler

@ContributesTo(AppScope::class)
@BindingContainer
interface AndroidTrackerBindings {
    @Binds
    val TrackLoginActivityRedirect.externalLoginRedirect: ExternalLoginRedirect

    @Binds
    val AndroidDelayedTrackingUpdateScheduler.delayedTrackingUpdateScheduler: DelayedTrackingUpdateScheduler
}
