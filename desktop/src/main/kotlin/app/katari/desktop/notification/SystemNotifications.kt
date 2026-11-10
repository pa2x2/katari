package app.katari.desktop.notification

import app.katari.core.common.platform.DesktopOperatingSystem
import app.katari.desktop.application.DesktopAppVariant
import app.katari.desktop.notification.freedesktop.DbusNotificationServer
import app.katari.desktop.notification.freedesktop.FreedesktopNotifications
import app.katari.desktop.notification.freedesktop.NotificationIconFile
import app.katari.desktop.notification.freedesktop.ShownNotificationsRecord
import java.nio.file.Path

/**
 * The operating system's notification service, which shows the app's notifications outside its window. Each
 * notification is shown under a key the app picks, and showing another under the same key replaces it where it is, as
 * Android replaces a notification by its id, including one an earlier run of the app showed. Calls return at once and are carried out in order on the service's own
 * thread, which also tells [Listener] what became of them.
 */
interface SystemNotifications {
    fun show(key: String, notification: DesktopNotification)

    /** Takes the notification under [key] away, if one shows, even one an earlier run of the app showed. */
    fun withdraw(key: String)

    /**
     * Takes away what shows work under way that an earlier run of the app left, which ended with that run, as when it
     * crashed: called as the app starts.
     */
    fun resume()

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

/**
 * The [SystemNotifications] of [operatingSystem], showing notifications as [app] with the app's icon and keeping
 * what a later run needs to know of them, and the icon where the service reads it, in [runtimeDirectory].
 */
fun systemNotifications(
    operatingSystem: DesktopOperatingSystem,
    app: DesktopAppVariant,
    runtimeDirectory: Path,
    listener: SystemNotifications.Listener,
): SystemNotifications = when (operatingSystem) {
    DesktopOperatingSystem.LINUX -> FreedesktopNotifications(
        app.displayName,
        app.applicationId,
        NotificationIconFile(runtimeDirectory.resolve("app-icon.png"))::uri,
        DbusNotificationServer::connect,
        ShownNotificationsRecord(runtimeDirectory.resolve("notifications.json")),
        listener,
    )
}
