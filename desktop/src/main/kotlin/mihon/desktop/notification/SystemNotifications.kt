package mihon.desktop.notification

import mihon.desktop.notification.freedesktop.DbusNotificationServer
import mihon.desktop.notification.freedesktop.FreedesktopNotifications
import mihon.desktop.platform.DesktopOperatingSystem

/**
 * The operating system's notification service, which shows the app's notifications outside its window. Each
 * notification is shown under a key the app picks, and showing another under the same key replaces it where it is, as
 * Android replaces a notification by its id. Calls return at once and are carried out in order on the service's own
 * thread, which also tells [Listener] what became of them.
 */
interface SystemNotifications {
    fun show(key: String, notification: DesktopNotification)

    /** Takes the notification under [key] away, if one shows. */
    fun withdraw(key: String)

    /**
     * Takes away the notifications of work under way, whose work ends with the app, and stops: called as the app
     * ends. Waits a moment for the service to take them.
     */
    fun close()

    interface Listener {
        /**
         * The user answered a notification with [response]. [activationToken] is what the desktop sent to let the app's
         * window take the focus for it, or null when it sent none.
         */
        fun answered(response: NotificationResponse, activationToken: String?)

        /** There was no notification service to show [notification] with. */
        fun unshown(notification: DesktopNotification)
    }
}

/** The [SystemNotifications] of [operatingSystem], showing notifications as [appName]. */
fun systemNotifications(
    operatingSystem: DesktopOperatingSystem,
    appName: String,
    listener: SystemNotifications.Listener,
): SystemNotifications = when (operatingSystem) {
    DesktopOperatingSystem.LINUX -> FreedesktopNotifications(appName, DbusNotificationServer::connect, listener)
}
