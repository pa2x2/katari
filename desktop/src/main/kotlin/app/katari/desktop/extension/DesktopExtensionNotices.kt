package app.katari.desktop.extension

import app.katari.desktop.home.HomeDestination
import app.katari.desktop.notification.DesktopNotification
import app.katari.desktop.notification.DesktopNotifications
import app.katari.desktop.notification.NotificationResponse
import app.katari.presentation.kit.feedback.AppMessages
import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.core.security.SecurityPreferences
import eu.kanade.tachiyomi.extension.platform.ExtensionNotices
import tachiyomi.i18n.MR
import tachiyomi.i18n.extension_api_error
import tachiyomi.i18n.extension_auto_update_notification_updated
import tachiyomi.i18n.localized.pluralStringResource
import tachiyomi.i18n.localized.stringResource
import tachiyomi.i18n.restore_extensions_installed
import tachiyomi.i18n.restore_extensions_missed
import tachiyomi.i18n.update_check_notification_ext_updates

/**
 * As on Android: an app message when the stores can't be reached, which Android shows as a toast, and notifications
 * for updates and a backup's reinstalled extensions, which open the extensions page. Extension names stay out of them
 * where the profile hides notification content.
 */
@Inject
class DesktopExtensionNotices(
    private val messages: AppMessages,
    private val notifications: DesktopNotifications,
    private val securityPreferences: SecurityPreferences,
) : ExtensionNotices {

    override fun storesUnreachable() {
        messages.show(stringResource(MR.strings.extension_api_error))
    }

    override fun autoUpdated(names: List<String>) {
        if (names.isEmpty()) {
            notifications.withdraw(AUTO_UPDATED)
            return
        }
        notifications.show(
            AUTO_UPDATED,
            extensionsNotification(
                pluralStringResource(MR.plurals.extension_auto_update_notification_updated, names.size, names.size),
                names.joinToString(", ").takeUnless { securityPreferences.hideNotificationContent.get() },
            ),
        )
    }

    override fun promptUpdates(names: List<String>) {
        if (names.isEmpty()) {
            notifications.withdraw(UPDATES)
            return
        }
        notifications.show(
            UPDATES,
            extensionsNotification(
                pluralStringResource(MR.plurals.update_check_notification_ext_updates, names.size, names.size),
                names.joinToString(", ").takeUnless { securityPreferences.hideNotificationContent.get() },
            ),
        )
    }

    override fun dismissUpdates() {
        notifications.withdraw(UPDATES)
        notifications.withdraw(AUTO_UPDATED)
    }

    override fun backupExtensionsInstalled(installed: List<String>, missed: List<String>) {
        notifications.show(
            BACKUP_EXTENSIONS,
            extensionsNotification(
                pluralStringResource(MR.plurals.restore_extensions_installed, installed.size, installed.size),
                missed.takeIf { it.isNotEmpty() && !securityPreferences.hideNotificationContent.get() }
                    ?.let { stringResource(MR.strings.restore_extensions_missed, it.joinToString(", ")) },
            ),
        )
    }

    private fun extensionsNotification(title: String, body: String?) = DesktopNotification(
        title = title,
        body = body,
        onClick = NotificationResponse.Open(HomeDestination.Extensions),
    )

    private companion object {
        const val UPDATES = "extension-updates"
        const val AUTO_UPDATED = "extensions-auto-updated"
        const val BACKUP_EXTENSIONS = "backup-extensions"
    }
}
