package eu.kanade.tachiyomi.extension.update

import android.content.Context
import androidx.core.app.NotificationCompat
import eu.kanade.tachiyomi.core.security.SecurityPreferences
import eu.kanade.tachiyomi.data.notification.NotificationReceiver
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.util.system.cancelNotification
import eu.kanade.tachiyomi.util.system.notify
import tachiyomi.i18n.*
import tachiyomi.i18n.context.pluralStringResource
import mihon.icons.materialsymbols.R as MaterialSymbolsR

class ExtensionUpdateNotifier(
    private val context: Context,
    private val securityPreferences: SecurityPreferences,
) {
    fun autoUpdated(names: List<String>) {
        if (names.isEmpty()) {
            context.cancelNotification(Notifications.ID_EXTENSIONS_AUTO_UPDATED)
            return
        }

        context.notify(
            Notifications.ID_EXTENSIONS_AUTO_UPDATED,
            Notifications.CHANNEL_EXTENSIONS_UPDATE,
        ) {
            setContentTitle(
                context.pluralStringResource(
                    MR.plurals.extension_auto_update_notification_updated,
                    names.size,
                    names.size,
                ),
            )
            if (!securityPreferences.hideNotificationContent.get()) {
                val extNames = names.joinToString(", ")
                setContentText(extNames)
                setStyle(NotificationCompat.BigTextStyle().bigText(extNames))
            }
            setSmallIcon(MaterialSymbolsR.drawable.rounded_filled_extension)
            setContentIntent(NotificationReceiver.openExtensionsPendingActivity(context))
            setAutoCancel(true)
        }
    }

    fun promptUpdates(names: List<String>) {
        if (names.isEmpty()) {
            context.cancelNotification(Notifications.ID_EXTENSION_UPDATES)
            return
        }

        context.notify(
            Notifications.ID_EXTENSION_UPDATES,
            Notifications.CHANNEL_EXTENSIONS_UPDATE,
        ) {
            setContentTitle(
                context.pluralStringResource(
                    MR.plurals.update_check_notification_ext_updates,
                    names.size,
                    names.size,
                ),
            )
            if (!securityPreferences.hideNotificationContent.get()) {
                val extNames = names.joinToString(", ")
                setContentText(extNames)
                setStyle(NotificationCompat.BigTextStyle().bigText(extNames))
            }
            setSmallIcon(MaterialSymbolsR.drawable.rounded_filled_extension)
            setContentIntent(NotificationReceiver.openExtensionsPendingActivity(context))
            setAutoCancel(true)
        }
    }

    fun dismiss() {
        context.cancelNotification(Notifications.ID_EXTENSION_UPDATES)
        context.cancelNotification(Notifications.ID_EXTENSIONS_AUTO_UPDATED)
    }
}
