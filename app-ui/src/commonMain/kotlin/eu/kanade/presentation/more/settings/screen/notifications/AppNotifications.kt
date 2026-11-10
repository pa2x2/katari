package eu.kanade.presentation.more.settings.screen.notifications

import app.katari.core.common.platform.PlatformFacility
import app.katari.core.platform.PlatformContext
import app.katari.notification.AppNotificationKind

/**
 * The notifications the app posts, such as for library updates and downloads. Where the platform shows none, the rows
 * about them are hidden.
 */
@PlatformFacility
interface AppNotifications {
    val isOffered: Boolean

    val choices: NotificationChoices
}

/** Where the person chooses which kinds of notification show. */
sealed interface NotificationChoices {
    /** The system keeps the choices, as Android does for each notification channel, in settings [open] shows. */
    fun interface InSystemSettings : NotificationChoices {
        fun open(context: PlatformContext)
    }

    /**
     * The app keeps them, in its Notifications settings, for the [kinds] this platform posts besides each profile's
     * library updates.
     */
    class InApp(val kinds: List<AppNotificationKind>) : NotificationChoices
}
