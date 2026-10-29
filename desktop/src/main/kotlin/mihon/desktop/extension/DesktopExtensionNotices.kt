package mihon.desktop.extension

import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.extension.platform.ExtensionNotices
import mihon.presentation.kit.feedback.AppMessages
import tachiyomi.i18n.MR
import tachiyomi.i18n.extension_api_error
import tachiyomi.i18n.extension_auto_update_notification_updated
import tachiyomi.i18n.localized.pluralStringResource
import tachiyomi.i18n.localized.stringResource
import tachiyomi.i18n.restore_extensions_installed
import tachiyomi.i18n.restore_extensions_missed
import tachiyomi.i18n.update_check_notification_ext_updates

/**
 * What Android shows as a toast or a notification, as app messages with the same wording. A message can't be taken
 * back, so withdrawn update notices just aren't shown again.
 */
@Inject
class DesktopExtensionNotices(private val messages: AppMessages) : ExtensionNotices {

    override fun storesUnreachable() {
        messages.show(stringResource(MR.strings.extension_api_error))
    }

    override fun autoUpdated(names: List<String>) {
        if (names.isEmpty()) return
        show(pluralStringResource(MR.plurals.extension_auto_update_notification_updated, names.size, names.size), names)
    }

    override fun promptUpdates(names: List<String>) {
        if (names.isEmpty()) return
        show(pluralStringResource(MR.plurals.update_check_notification_ext_updates, names.size, names.size), names)
    }

    override fun dismissUpdates() = Unit

    override fun backupExtensionsInstalled(installed: List<String>, missed: List<String>) {
        val lines = buildList {
            add(pluralStringResource(MR.plurals.restore_extensions_installed, installed.size, installed.size))
            if (missed.isNotEmpty()) {
                add(
                    stringResource(MR.strings.restore_extensions_missed, missed.joinToString(", ")),
                )
            }
        }
        messages.show(lines.joinToString("\n"), long = true)
    }

    private fun show(title: String, names: List<String>) {
        messages.show("$title\n${names.joinToString(", ")}", long = true)
    }
}
