package mihon.desktop.notification

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import mihon.core.metro.AppCoroutineScope
import mihon.desktop.application.DesktopAppVariant
import mihon.desktop.display.backend.DisplayBackend
import mihon.desktop.home.HomeDestinations
import mihon.desktop.platform.DesktopOperatingSystem
import mihon.desktop.window.OpenWindow
import mihon.presentation.kit.feedback.AppMessages

/**
 * The app's notifications on the desktop, through the operating system's [SystemNotifications], which starts with the
 * first one. Where the desktop has no notification service, a result shows as one of the app's [messages] instead, as
 * desktop showed them before it posted notifications, and work under way isn't shown: the screens show its progress.
 * Answering a notification by opening something brings the window forward with the activation token the desktop sent,
 * as a later launch of the app does.
 */
@Inject
@SingleIn(AppScope::class)
class DesktopNotifications(
    operatingSystem: DesktopOperatingSystem,
    private val messages: AppMessages,
    private val openWindow: OpenWindow,
    private val displayBackend: DisplayBackend,
    // Opening one switches profiles, and the profile manager sets up work that posts notifications
    private val destinations: Lazy<HomeDestinations>,
    @AppCoroutineScope private val scope: CoroutineScope,
) {
    private val system = lazy {
        systemNotifications(operatingSystem, DesktopAppVariant.current.displayName, Answers())
    }

    /** Shows [notification] under [key], in place of the one shown under it. */
    fun show(key: String, notification: DesktopNotification) = system.value.show(key, notification)

    fun withdraw(key: String) {
        if (system.isInitialized()) system.value.withdraw(key)
    }

    /** Takes away what shows work under way, which ends with the app. Called as the app ends. */
    fun close() {
        if (system.isInitialized()) system.value.close()
    }

    private inner class Answers : SystemNotifications.Listener {
        override fun answered(response: NotificationResponse, activationToken: String?) {
            when (response) {
                is NotificationResponse.Open -> scope.launch(Dispatchers.Main) {
                    openWindow.context?.window?.let { displayBackend.bringForward(it, activationToken) }
                    destinations.value.open(response.destination)
                }
                is NotificationResponse.Run -> scope.launch { response.action() }
            }
        }

        override fun unshown(notification: DesktopNotification) {
            if (notification.isOngoing) return
            messages.show(listOfNotNull(notification.title, notification.body).joinToString("\n"), long = true)
        }
    }
}
