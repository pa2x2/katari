package app.katari.desktop.notification

import app.katari.core.common.platform.DesktopOperatingSystem
import app.katari.core.metro.AppCoroutineScope
import app.katari.desktop.application.DesktopAppVariant
import app.katari.desktop.home.HomeDestinations
import app.katari.desktop.storage.DesktopDirectories
import app.katari.desktop.window.MainWindowVisibility
import app.katari.notification.NotificationKind
import app.katari.notification.NotificationPreferences
import app.katari.presentation.kit.feedback.AppMessages
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * The app's notifications on the desktop, through the operating system's [SystemNotifications], which starts with the
 * first one. Where the desktop has no notification service, a result shows as one of the app's [messages] instead, as
 * desktop showed them before it posted notifications, and work under way isn't shown: the screens show its progress.
 * Answering a notification by opening something brings the window forward with the activation token the desktop sent,
 * hidden in the tray or not, as a later launch of the app does.
 *
 * Desktops keep no settings for an app's kinds of notification, as Android does for the app and its channels, so the
 * app keeps them in [preferences]: with notifications turned off, or of a kind turned off, a notification isn't shown,
 * and turning either off takes away what shows. The work it tells of goes on, as where the desktop has no notification
 * service.
 */
@Inject
@SingleIn(AppScope::class)
class DesktopNotifications(
    operatingSystem: DesktopOperatingSystem,
    directories: DesktopDirectories,
    private val messages: AppMessages,
    private val preferences: NotificationPreferences,
    private val mainWindow: MainWindowVisibility,
    // Opening one switches profiles, and the profile manager sets up work that posts notifications
    private val destinations: Lazy<HomeDestinations>,
    @AppCoroutineScope private val scope: CoroutineScope,
) {
    private val system = lazy {
        systemNotifications(operatingSystem, DesktopAppVariant.current, directories.runtime, Answers())
    }

    // What shows this run under each key, and the kinds whose switch is watched, to take away a kind turned off
    private val shownKinds = ConcurrentHashMap<String, NotificationKind>()
    private val watchedKinds = ConcurrentHashMap.newKeySet<NotificationKind>()

    init {
        scope.launch {
            preferences.enabled.changes().filter { !it }.collect { shownKinds.keys.forEach(::withdraw) }
        }
    }

    /**
     * Shows [notification] under [key], in place of the one shown under it. With notifications or the notification's
     * kind turned off, it takes away the one shown under [key] instead.
     */
    fun show(key: String, notification: DesktopNotification) {
        val shows = preferences.shows(notification.kind)
        if (watchedKinds.add(notification.kind)) {
            scope.launch {
                shows.changes().filter { !it }.collect { withdrawAll(notification.kind) }
            }
        }
        if (!preferences.enabled.get() || !shows.get()) {
            withdraw(key)
            return
        }
        shownKinds[key] = notification.kind
        system.value.show(key, notification)
    }

    fun withdraw(key: String) {
        shownKinds.remove(key)
        if (system.isInitialized()) system.value.withdraw(key)
    }

    private fun withdrawAll(kind: NotificationKind) {
        shownKinds.filterValues { it == kind }.keys.forEach(::withdraw)
    }

    /** Takes away what shows work under way that an earlier run left. Called as the app starts. */
    fun resume() = system.value.resume()

    /** Takes away what shows work under way, which ends with the app. Called as the app ends. */
    fun close() {
        if (system.isInitialized()) system.value.close()
    }

    private inner class Answers : SystemNotifications.Listener {
        override fun answered(response: NotificationResponse, activationToken: String?) {
            when (response) {
                is NotificationResponse.Open -> scope.launch(Dispatchers.Main) {
                    mainWindow.bringForward(activationToken)
                    destinations.value.open(response.destination)
                }
                is NotificationResponse.Show -> scope.launch(Dispatchers.Main) {
                    mainWindow.bringForward(activationToken)
                    response.action()
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
