package eu.kanade.presentation.more.settings.screen.notifications

import app.katari.notification.NotificationPreferences

/** Graph entry points of the notification settings. */
interface NotificationSettingsGraph {
    val appNotifications: AppNotifications
    val notificationPreferences: NotificationPreferences
}
