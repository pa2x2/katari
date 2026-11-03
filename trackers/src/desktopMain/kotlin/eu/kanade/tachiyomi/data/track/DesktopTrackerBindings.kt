package eu.kanade.tachiyomi.data.track

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.ContributesTo
import eu.kanade.tachiyomi.data.track.oauth.ExternalLoginRedirect
import eu.kanade.tachiyomi.data.track.oauth.NoExternalLoginRedirect

@ContributesTo(AppScope::class)
@BindingContainer
interface DesktopTrackerBindings {
    @Binds
    val NoExternalLoginRedirect.externalLoginRedirect: ExternalLoginRedirect
}
