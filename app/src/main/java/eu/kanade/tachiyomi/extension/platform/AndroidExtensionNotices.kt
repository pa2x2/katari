package eu.kanade.tachiyomi.extension.platform

import android.content.Context
import androidx.core.app.NotificationCompat
import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.core.security.SecurityPreferences
import eu.kanade.tachiyomi.data.notification.NotificationReceiver
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.extension.update.ExtensionUpdateNotifier
import eu.kanade.tachiyomi.util.system.notify
import eu.kanade.tachiyomi.util.system.toast
import tachiyomi.i18n.*
import tachiyomi.i18n.context.pluralStringResource
import tachiyomi.i18n.context.stringResource
import app.katari.icons.materialsymbols.R as MaterialSymbolsR

/** A toast for the stores, and notifications for updates and a backup's reinstalled extensions. */
@Inject
class AndroidExtensionNotices(
    private val context: Context,
    private val securityPreferences: SecurityPreferences,
) : ExtensionNotices {

    private val updateNotifier by lazy { ExtensionUpdateNotifier(context, securityPreferences) }

    override fun storesUnreachable() {
        context.toast(context.stringResource(MR.strings.extension_api_error))
    }

    override fun autoUpdated(names: List<String>) {
        updateNotifier.autoUpdated(names)
    }

    override fun promptUpdates(names: List<String>) {
        updateNotifier.promptUpdates(names)
    }

    override fun dismissUpdates() {
        updateNotifier.dismiss()
    }

    override fun backupExtensionsInstalled(installed: List<String>, missed: List<String>) {
        context.notify(Notifications.ID_RESTORE_EXTENSIONS, Notifications.CHANNEL_BACKUP_RESTORE_COMPLETE) {
            setContentTitle(
                context.pluralStringResource(
                    MR.plurals.restore_extensions_installed,
                    installed.size,
                    installed.size,
                ),
            )
            if (missed.isNotEmpty() && !securityPreferences.hideNotificationContent.get()) {
                val text = context.stringResource(MR.strings.restore_extensions_missed, missed.joinToString(", "))
                setContentText(text)
                setStyle(NotificationCompat.BigTextStyle().bigText(text))
            }
            setSmallIcon(MaterialSymbolsR.drawable.rounded_filled_extension)
            setContentIntent(NotificationReceiver.openExtensionsPendingActivity(context))
            setAutoCancel(true)
        }
    }
}
