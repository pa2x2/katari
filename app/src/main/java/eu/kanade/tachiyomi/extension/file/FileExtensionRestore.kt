package eu.kanade.tachiyomi.extension.file

import android.content.Context
import androidx.core.app.NotificationCompat
import eu.kanade.tachiyomi.core.security.SecurityPreferences
import eu.kanade.tachiyomi.data.notification.NotificationReceiver
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.extension.installer.ExtensionInstallOperation
import eu.kanade.tachiyomi.extension.installer.routing
import eu.kanade.tachiyomi.extension.model.Extension
import eu.kanade.tachiyomi.extension.model.InstallStep
import eu.kanade.tachiyomi.extension.store.isInstallable
import eu.kanade.tachiyomi.extension.util.ExtensionInstaller
import eu.kanade.tachiyomi.util.system.notify
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.i18n.*
import tachiyomi.i18n.context.pluralStringResource
import tachiyomi.i18n.context.stringResource
import mihon.icons.materialsymbols.R as MaterialSymbolsR

/** A file extension a restored backup had installed, which [FileExtensionRestore] installs again. */
data class RestoredFileExtension(
    val pkgName: String,
    val name: String,
    val storeIndexUrl: String,
)

/**
 * Installs the file extensions of a restored backup from the stores they came from, in the background once the
 * restored stores' listings are in, then says in one notification what was installed and what couldn't be.
 */
internal class FileExtensionRestore(
    private val context: Context,
    private val installer: ExtensionInstaller,
    private val securityPreferences: SecurityPreferences,
) {

    /** [listings] are every store's, fetched after the stores were restored; [installed] are on the device already. */
    suspend fun install(
        restored: List<RestoredFileExtension>,
        listings: List<Extension.Available>,
        isInstalled: (pkgName: String) -> Boolean,
    ) {
        val installed = mutableListOf<String>()
        val missed = mutableListOf<String>()
        restored.filterNot { isInstalled(it.pkgName) }.forEach { extension ->
            val listing = listings.firstOrNull {
                it.pkgName == extension.pkgName && it.store.indexUrl == extension.storeIndexUrl && it.isInstallable
            }
            if (listing != null && install(listing) == InstallStep.Installed) {
                installed += extension.name
            } else {
                missed += extension.name
            }
        }
        notify(installed, missed)
    }

    private suspend fun install(listing: Extension.Available): InstallStep {
        val routing = ExtensionInstallOperation.NewInstall.routing()
        return try {
            installer.downloadAndInstall(listing, routing.userActionBehavior, routing.installRoute)
                .first { it.isCompleted() }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e) { "Failed to install ${listing.pkgName} from a backup" }
            InstallStep.Error.from(e)
        }
    }

    private fun notify(installed: List<String>, missed: List<String>) {
        if (installed.isEmpty() && missed.isEmpty()) return
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
