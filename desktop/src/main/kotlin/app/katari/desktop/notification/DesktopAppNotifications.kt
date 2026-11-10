package app.katari.desktop.notification

import app.katari.notification.AppNotificationKind
import dev.zacsweers.metro.Inject
import eu.kanade.presentation.more.settings.screen.notifications.AppNotifications
import eu.kanade.presentation.more.settings.screen.notifications.NotificationChoices

/**
 * Desktop posts notifications through the desktop's notification service. Desktops keep no settings for an app's
 * kinds of notification, as Android's channels have, so the app keeps them, and [DesktopNotifications] follows them.
 */
@Inject
class DesktopAppNotifications : AppNotifications {
    override val isOffered: Boolean = true

    override val choices = NotificationChoices.InApp(
        // Desktop posts none for background translation, video playback or incognito mode
        AppNotificationKind.entries - setOf(
            AppNotificationKind.TRANSLATION_PROGRESS,
            AppNotificationKind.VIDEO_PLAYBACK,
            AppNotificationKind.INCOGNITO_MODE,
        ),
    )
}
