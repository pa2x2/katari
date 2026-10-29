package eu.kanade.tachiyomi.data.track.oauth

import dev.zacsweers.metro.Inject

/**
 * Desktop has nothing yet that receives a tracker site's login redirect, so the trackers that log in on their site
 * (OAuth) are not offered; those that take a username and password, or none, are.
 */
@Inject
class NoExternalLoginRedirect : ExternalLoginRedirect {
    override val isAvailable: Boolean = false
}
