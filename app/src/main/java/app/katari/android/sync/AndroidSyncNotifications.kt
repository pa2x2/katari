package app.katari.android.sync

import android.app.Application
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import app.katari.notification.AppNotificationKind
import app.katari.sync.client.connection.SyncStop
import app.katari.sync.ui.notice.DownloadRequestFailure
import app.katari.sync.ui.notice.SyncNotifications
import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.notification.NotificationReceiver
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.util.system.cancelNotification
import eu.kanade.tachiyomi.util.system.notificationBuilder
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

    override fun showTestMessage(from: String?) {
        val device = from ?: application.stringResource(MR.strings.sync_test_message_unknown_device)
        notify(
            id = Notifications.ID_SYNC_TEST_MESSAGE,
            title = application.stringResource(MR.strings.sync_title),
            text = application.stringResource(MR.strings.sync_test_message_received, device),
            action = SyncNavigation.ACTION_OPEN_SYNC,
        )
    }

    override fun showDownloadRequestFailed(failure: DownloadRequestFailure) {
        val from = failure.from ?: application.stringResource(MR.strings.sync_download_request_unknown_device)
        val chapters = failure.chapters.singleOrNull() ?: application.pluralStringResource(
            MR.plurals.sync_download_request_and_more,
            failure.chapters.size - 1,
            failure.chapters.first(),
            failure.chapters.size - 1,
        )
        val text = when (failure.reason) {
            DownloadRequestFailure.Reason.SOURCE_MISSING -> MR.strings.sync_download_request_missing_source
            DownloadRequestFailure.Reason.CHAPTERS_GONE -> MR.strings.sync_download_request_missing_chapters
        }
        val notification = application.notificationBuilder(AppNotificationKind.SYNC.id) {
            setSmallIcon(R.drawable.ic_katari)
            setContentTitle(application.stringResource(MR.strings.sync_download_request_failed_title, failure.title))
            setContentText(application.stringResource(text, from, chapters, failure.sourceName))
            setStyle(NotificationCompat.BigTextStyle())
            setAutoCancel(true)
            setContentIntent(
                NotificationReceiver.openEntryPendingActivity(application, failure.profileId, failure.entryId),
            )
        }.build()
        // One per entry, apart from the entry's other notifications by its tag
        application.notify(
            listOf(
                NotificationManagerCompat.NotificationWithIdAndTag(
                    DOWNLOAD_REQUEST_TAG,
                    failure.entryId.hashCode(),
                    notification,
                ),
            ),
        )
    }

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

    private companion object {
        const val DOWNLOAD_REQUEST_TAG = "sync-download-request"
    }
}
