package mihon.desktop.advanced

import dev.zacsweers.metro.Inject
import eu.kanade.presentation.more.settings.screen.advanced.AppNotifications
import mihon.core.platform.PlatformContext

/**
 * Desktop posts no notifications yet, so there are none to manage or hide. Stands in until desktop notifies through
 * the system.
 */
@Inject
class NoAppNotifications : AppNotifications {
    override val isOffered: Boolean = false

    override fun openSystemSettings(context: PlatformContext) =
        error("Desktop posts no notifications whose settings could be opened")
}
