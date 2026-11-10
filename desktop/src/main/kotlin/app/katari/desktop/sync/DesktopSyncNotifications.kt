package app.katari.desktop.sync

import app.katari.desktop.home.HomeDestination
import app.katari.desktop.notification.DesktopNotification
import app.katari.desktop.notification.DesktopNotifications
import app.katari.desktop.notification.NotificationResponse
import app.katari.notification.AppNotificationKind
import app.katari.sync.client.connection.SyncStop
import app.katari.sync.ui.notice.SyncNotifications
import dev.zacsweers.metro.Inject
import okhttp3.HttpUrl
import tachiyomi.i18n.MR
import tachiyomi.i18n.localized.pluralStringResource
import tachiyomi.i18n.localized.stringResource
import tachiyomi.i18n.sync_account_disabled_title
import tachiyomi.i18n.sync_ended_text
import tachiyomi.i18n.sync_notification_reset_text
import tachiyomi.i18n.sync_questions_waiting
import tachiyomi.i18n.sync_removed_title
import tachiyomi.i18n.sync_status_server_reset
import tachiyomi.i18n.sync_status_sign_in_again
import tachiyomi.i18n.sync_title

@Inject
class DesktopSyncNotifications(private val notifications: DesktopNotifications) : SyncNotifications {
    override fun showQuestions(count: Int) {
        notifications.show(
            QUESTIONS,
            DesktopNotification(
                kind = AppNotificationKind.SYNC,
                title = stringResource(MR.strings.sync_title),
                body = pluralStringResource(MR.plurals.sync_questions_waiting, count, count),
                onClick = NotificationResponse.Open(HomeDestination.SyncQuestions),
            ),
        )
    }

    override fun withdrawQuestions() = notifications.withdraw(QUESTIONS)

    override fun showStopped(reason: SyncStop, server: HttpUrl?) {
        val ended = server?.let { stringResource(MR.strings.sync_ended_text, it.host) }
        val (title, body) = when (reason) {
            SyncStop.REMOVED -> stringResource(MR.strings.sync_removed_title) to ended
            SyncStop.ACCOUNT_DISABLED -> stringResource(MR.strings.sync_account_disabled_title) to ended
            SyncStop.SERVER_RESET ->
                stringResource(MR.strings.sync_status_server_reset) to
                    stringResource(MR.strings.sync_notification_reset_text)
            else -> stringResource(MR.strings.sync_title) to stringResource(MR.strings.sync_status_sign_in_again)
        }
        notifications.show(
            STOPPED,
            DesktopNotification(
                kind = AppNotificationKind.SYNC,
                title,
                body,
                onClick = NotificationResponse.Open(HomeDestination.SyncPage),
            ),
        )
    }

    override fun withdrawStopped() = notifications.withdraw(STOPPED)

    private companion object {
        const val QUESTIONS = "sync-questions"
        const val STOPPED = "sync-stopped"
    }
}
