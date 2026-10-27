package mihon.desktop.di

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import eu.kanade.tachiyomi.data.track.TrackerFailureMessages
import eu.kanade.tachiyomi.data.track.oauth.ExternalLoginRedirect
import eu.kanade.tachiyomi.data.track.source.TrackerSourceAccess
import mihon.desktop.tracking.NoExternalLoginRedirect
import mihon.desktop.tracking.NoTrackerFailureMessages
import mihon.desktop.tracking.NoTrackerSourceAccess

@BindingContainer
interface TrackerBindings {
    @Binds
    val NoTrackerSourceAccess.trackerSourceAccess: TrackerSourceAccess

    @Binds
    val NoExternalLoginRedirect.externalLoginRedirect: ExternalLoginRedirect

    @Binds
    val NoTrackerFailureMessages.trackerFailureMessages: TrackerFailureMessages
}
