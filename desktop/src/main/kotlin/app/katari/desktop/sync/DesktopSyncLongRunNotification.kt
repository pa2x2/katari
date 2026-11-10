package app.katari.desktop.sync

import app.katari.desktop.home.HomeDestination
import app.katari.desktop.notification.DesktopNotification
import app.katari.desktop.notification.DesktopNotifications
import app.katari.desktop.notification.NotificationProgress
import app.katari.desktop.notification.NotificationResponse
import app.katari.desktop.notification.NotificationUrgency
import app.katari.notification.AppNotificationKind
import app.katari.sync.client.status.SyncStatus
import app.katari.sync.ui.notice.SyncLongRunNotice
import dev.zacsweers.metro.Inject
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
                kind = AppNotificationKind.SYNC_PROGRESS,
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
