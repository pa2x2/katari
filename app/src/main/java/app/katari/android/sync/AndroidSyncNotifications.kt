package app.katari.android.sync

import android.app.Application
import androidx.core.app.NotificationCompat
import app.katari.notification.AppNotificationKind
import app.katari.sync.client.connection.SyncStop
import app.katari.sync.ui.notice.SyncNotifications
import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.util.system.cancelNotification
import eu.kanade.tachiyomi.util.system.notify
import okhttp3.HttpUrl
import tachiyomi.i18n.*
import tachiyomi.i18n.context.pluralStringResource
import tachiyomi.i18n.context.stringResource

@Inject
class AndroidSyncNotifications(private val application: Application) : SyncNotifications {
    override fun showQuestions(count: Int) {
        notify(
            id = Notifications.ID_SYNC_QUESTIONS,
            title = application.stringResource(MR.strings.sync_title),
            text = application.pluralStringResource(MR.plurals.sync_questions_waiting, count, count),
            action = SyncNavigation.ACTION_OPEN_SYNC_QUESTIONS,
        )
    }

    override fun withdrawQuestions() = application.cancelNotification(Notifications.ID_SYNC_QUESTIONS)

    override fun showStopped(reason: SyncStop, server: HttpUrl?) {
        val ended = server?.let { application.stringResource(MR.strings.sync_ended_text, it.host) }
        val (title, text) = when (reason) {
            SyncStop.REMOVED -> application.stringResource(MR.strings.sync_removed_title) to ended
            SyncStop.ACCOUNT_DISABLED -> application.stringResource(MR.strings.sync_account_disabled_title) to ended
            SyncStop.SERVER_RESET -> application.stringResource(MR.strings.sync_status_server_reset) to
                application.stringResource(MR.strings.sync_notification_reset_text)
            else -> application.stringResource(MR.strings.sync_title) to
                application.stringResource(MR.strings.sync_status_sign_in_again)
        }
        notify(Notifications.ID_SYNC_STOPPED, title, text, SyncNavigation.ACTION_OPEN_SYNC)
    }

    override fun withdrawStopped() = application.cancelNotification(Notifications.ID_SYNC_STOPPED)

    private fun notify(id: Int, title: String, text: String?, action: String) {
        application.notify(id, AppNotificationKind.SYNC.id) {
            setSmallIcon(R.drawable.ic_katari)
            setContentTitle(title)
            setContentText(text)
            setStyle(NotificationCompat.BigTextStyle())
            setAutoCancel(true)
            setContentIntent(SyncNavigation.pendingIntent(application, action, id))
        }
    }
}
