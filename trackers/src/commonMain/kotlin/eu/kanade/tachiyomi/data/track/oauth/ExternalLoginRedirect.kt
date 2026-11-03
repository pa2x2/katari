package eu.kanade.tachiyomi.data.track.oauth

import mihon.core.common.platform.PlatformFacility

/**
 * The app's side of an [eu.kanade.tachiyomi.data.track.ExternalLoginTracker]'s login: the service's site sends the
 * result to the tracker's redirect address, which the platform has to route back to the app. Where nothing receives it
 * yet, [isAvailable] is false and external logins are not offered.
 */
@PlatformFacility
interface ExternalLoginRedirect {
    val isAvailable: Boolean
}
