package eu.kanade.presentation.more.settings.screen.advanced

import app.katari.core.common.platform.PlatformFacility
import app.katari.core.platform.PlatformContext

/**
 * The notifications the app posts, such as for library updates and downloads. Where the platform shows none, the rows
 * about them are hidden.
 */
@PlatformFacility
interface AppNotifications {
    val isOffered: Boolean

    /**
     * Whether the system has settings of its own for the app's notifications, as Android's per-channel settings, that
     * [openSystemSettings] opens.
     */
    val hasSystemSettings: Boolean

    /** Only called while [hasSystemSettings]. */
    fun openSystemSettings(context: PlatformContext)
}
