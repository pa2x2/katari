package app.katari.desktop.update

import app.katari.appupdate.AppUpdateController
import app.katari.core.metro.AppCoroutineScope
import app.katari.desktop.notification.DesktopNotification
import app.katari.desktop.notification.DesktopNotifications
import app.katari.desktop.notification.NotificationResponse
import app.katari.desktop.window.MainWindowVisibility
import app.katari.notification.AppNotificationKind
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import tachiyomi.i18n.MR
import tachiyomi.i18n.app_update_available
import tachiyomi.i18n.app_update_version_available
import tachiyomi.i18n.localized.stringResource

/**
 * Tells of an update whose prompt opened while the window is hidden in the tray, where nobody sees the prompt. Clicking
 * it brings the window forward with the prompt. Once a run for each version: a version the user has seen the prompt of,
 * or been told of, isn't told of again when the window hides once more or a later check finds it again. The
 * notification goes away once the window shows or the prompt closes.
 */
@Inject
class AppUpdateTrayNotification(
    private val controller: AppUpdateController,
    private val mainWindow: MainWindowVisibility,
    private val notifications: DesktopNotifications,
    @AppCoroutineScope private val scope: CoroutineScope,
) {
    fun start() {
        scope.launch {
            var toldVersion: String? = null
            combine(controller.state, mainWindow.isHidden) { state, hidden ->
                // The prompt stays open through a recheck, which mustn't take the notification away
                state.update?.version?.takeIf { state.sheetOpen } to hidden
            }.distinctUntilChanged().collect { (version, hidden) ->
                when {
                    version == null -> notifications.withdraw(KEY)
                    !hidden -> {
                        toldVersion = version
                        notifications.withdraw(KEY)
                    }
                    version != toldVersion -> {
                        toldVersion = version
                        notifications.show(KEY, notification(version))
                    }
                }
            }
        }
    }

    private fun notification(version: String) = DesktopNotification(
        kind = AppNotificationKind.COMMON,
        title = stringResource(MR.strings.app_update_available),
        body = stringResource(MR.strings.app_update_version_available, version),
        onClick = NotificationResponse.Show(controller::openSheet),
    )

    private companion object {
        const val KEY = "app-update"
    }
}
