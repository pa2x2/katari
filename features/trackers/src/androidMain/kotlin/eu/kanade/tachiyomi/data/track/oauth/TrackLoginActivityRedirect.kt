package eu.kanade.tachiyomi.data.track.oauth

import dev.zacsweers.metro.Inject

/** The app's TrackLoginActivity handles every tracker's redirect address. */
@Inject
class TrackLoginActivityRedirect : ExternalLoginRedirect {
    override val isAvailable: Boolean = true
}
