package eu.kanade.presentation.more.settings.screen.advanced

import mihon.core.platform.PlatformContext

/**
 * The notifications the app posts, such as for library updates and downloads, whose channels the system's own
 * settings manage. Where the platform shows none, the rows about them are hidden.
 */
interface AppNotifications {
    val isOffered: Boolean

    /** Opens the system's settings of the app's notifications. Only called while [isOffered]. */
    fun openSystemSettings(context: PlatformContext)
}
