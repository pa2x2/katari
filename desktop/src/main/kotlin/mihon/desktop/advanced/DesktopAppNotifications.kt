package mihon.desktop.advanced

import dev.zacsweers.metro.Inject
import eu.kanade.presentation.more.settings.screen.advanced.AppNotifications
import mihon.core.platform.PlatformContext

/**
 * Desktop posts notifications through the desktop's notification service. Desktops keep no settings for an app's
 * kinds of notification, as Android's channels have, so there are no system settings to open.
 */
@Inject
class DesktopAppNotifications : AppNotifications {
    override val isOffered: Boolean = true

    override val hasSystemSettings: Boolean = false

    override fun openSystemSettings(context: PlatformContext) =
        error("The desktop has no settings of the app's notifications to open")
}
