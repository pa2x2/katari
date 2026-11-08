package mihon.desktop.sync

import app.katari.sync.client.status.SyncStatus
import app.katari.sync.ui.notice.SyncLongRunNotice
import dev.zacsweers.metro.Inject
import mihon.desktop.home.HomeDestination
import mihon.desktop.notification.DesktopNotification
import mihon.desktop.notification.DesktopNotifications
import mihon.desktop.notification.NotificationProgress
import mihon.desktop.notification.NotificationResponse
import mihon.desktop.notification.NotificationUrgency
import tachiyomi.i18n.MR
import tachiyomi.i18n.localized.stringResource
import tachiyomi.i18n.sync_long_run_progress

/** The notification a long run shows while it goes, as Android's does, opening the Sync page. */
@Inject
class DesktopSyncLongRunNotification(private val notifications: DesktopNotifications) {
    fun show(status: SyncStatus) {
        val (title, server, percent) = SyncLongRunNotice.of(status)
        notifications.show(
            KEY,
            DesktopNotification(
                title = stringResource(title),
                body = when {
                    server != null && percent != null -> stringResource(
                        MR.strings.sync_long_run_progress,
                        server,
                        percent,
                    )
                    else -> server
                },
                progress = NotificationProgress(percent ?: 0, if (percent != null) PERCENT else 0),
                urgency = NotificationUrgency.Low,
                onClick = NotificationResponse.Open(HomeDestination.SyncPage),
            ),
        )
    }

    fun withdraw() = notifications.withdraw(KEY)

    private companion object {
        const val KEY = "sync-long-run"
        const val PERCENT = 100
    }
}
