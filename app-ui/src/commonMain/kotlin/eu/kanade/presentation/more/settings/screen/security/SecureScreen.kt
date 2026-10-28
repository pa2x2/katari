package eu.kanade.presentation.more.settings.screen.security

/**
 * Keeping the app's screens out of screenshots, screen recordings and the system's list of recent apps. Where the
 * platform can't, the security settings hide the option.
 */
interface SecureScreen {
    val isOffered: Boolean
}
