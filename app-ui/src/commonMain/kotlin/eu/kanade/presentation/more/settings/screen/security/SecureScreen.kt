package eu.kanade.presentation.more.settings.screen.security

import app.katari.core.common.platform.PlatformFacility

/**
 * Keeping the app's screens out of screenshots, screen recordings and the system's list of recent apps. Where the
 * platform can't, the security settings hide the option.
 */
@PlatformFacility
interface SecureScreen {
    val isOffered: Boolean
}
