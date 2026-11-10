package app.katari.entry.interactions.download.notification

import android.Manifest
import android.app.Notification
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.PermissionChecker
import app.katari.entry.interactions.download.EntryDownloadEntryIdentity
import app.katari.entry.interactions.download.EntryDownloadErrorNotification
import app.katari.entry.interactions.download.EntryDownloadNotificationActions
import app.katari.entry.interactions.download.EntryDownloadNotificationPresenter
import app.katari.entry.interactions.download.EntryDownloadNotifications
import app.katari.entry.interactions.download.EntryDownloadProgressNotification
import app.katari.notification.AppNotificationKind
import tachiyomi.i18n.*
import tachiyomi.i18n.context.stringResource

data class EntryDownloadNotificationLabels(
    val downloader: String,
    val unknownError: String,
    val paused: String,
    val downloadsPaused: String,
    val pause: String,
    val resume: String,
    val cancelAll: String,
    val showEntry: String,
)

interface EntryDownloadNotificationPublisher {
    fun notify(id: Int, notification: Notification)
    fun cancel(id: Int)
}

class AndroidEntryDownloadNotifier(
    private val context: Context,
    private val actions: EntryDownloadNotificationActions,
    private val hideNotificationContent: () -> Boolean,
    private val labels: EntryDownloadNotificationLabels = context.entryDownloadNotificationLabels(),
    private val publisher: EntryDownloadNotificationPublisher = AndroidEntryDownloadNotificationPublisher(context),
) : EntryDownloadNotificationPresenter {
    // False until the first progress after a pause or completion, which also clears a paused notification that may
    // have outlived an earlier process
    @Volatile
    private var showingProgress = false

    override fun showProgress(content: EntryDownloadProgressNotification) {
        if (!showingProgress) {
            publisher.cancel(EntryDownloadNotifications.ID_PAUSED)
            showingProgress = true
        }
        val hideContent = hideNotificationContent()
        notificationBuilder(AppNotificationKind.DOWNLOAD_PROGRESS.id) {
            setSmallIcon(android.R.drawable.stat_sys_download)
            setAutoCancel(false)
            setOnlyAlertOnce(true)
            setOngoing(true)
            setContentIntent(actions.openDownloadManager(context))
            setContentTitle(
                if (hideContent) {
                    content.hiddenTitle ?: labels.downloader
                } else {
                    content.title
                },
            )
            setContentText(content.text.takeUnless { hideContent })
            setProgress(
                content.maximum,
                content.current.coerceIn(0, content.maximum),
                content.indeterminate,
            )
            addActiveActions(content.destination)
        }.show(EntryDownloadNotifications.ID_PROGRESS)
    }

    override fun showPaused() {
        showingProgress = false
        notificationBuilder(AppNotificationKind.DOWNLOAD_PROGRESS.id) {
            setContentTitle(labels.paused)
            setContentText(labels.downloadsPaused)
            setSmallIcon(android.R.drawable.ic_media_pause)
            setAutoCancel(false)
            setOnlyAlertOnce(true)
            setProgress(0, 0, false)
            setOngoing(false)
            setContentIntent(actions.openDownloadManager(context))
            addAction(
                android.R.drawable.ic_media_play,
                labels.resume,
                actions.resumeDownloads(context),
            )
            addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                labels.cancelAll,
                actions.clearDownloads(context),
            )
        }.show(EntryDownloadNotifications.ID_PAUSED)
    }

    override fun onComplete() {
        showingProgress = false
        publisher.cancel(EntryDownloadNotifications.ID_PROGRESS)
        publisher.cancel(EntryDownloadNotifications.ID_PAUSED)
    }

    override fun showError(content: EntryDownloadErrorNotification) {
        val hideContent = hideNotificationContent()
        notificationBuilder(AppNotificationKind.DOWNLOAD_ERRORS.id) {
            setContentTitle(content.title.takeUnless { hideContent } ?: labels.downloader)
            setContentText(content.message.takeUnless { hideContent } ?: labels.unknownError)
            setSmallIcon(android.R.drawable.stat_sys_warning)
            setAutoCancel(true)
            setContentIntent(actions.openDownloadManager(context))
            content.destination?.let { addOpenEntryAction(it) }
        }.show(EntryDownloadNotifications.ID_ERROR)
    }

    override fun showWarning(reason: String, timeoutMillis: Long?, helpUrl: String?) {
        notificationBuilder(AppNotificationKind.DOWNLOAD_ERRORS.id) {
            setContentTitle(labels.downloader)
            setStyle(NotificationCompat.BigTextStyle().bigText(reason))
            setSmallIcon(android.R.drawable.stat_sys_warning)
            setAutoCancel(true)
            setContentIntent(helpUrl?.let { actions.openUrl(context, it) } ?: actions.openDownloadManager(context))
            timeoutMillis?.let(::setTimeoutAfter)
        }.show(EntryDownloadNotifications.ID_ERROR)
    }

    private fun NotificationCompat.Builder.addActiveActions(destination: EntryDownloadEntryIdentity) {
        addAction(
            android.R.drawable.ic_media_pause,
            labels.pause,
            actions.pauseDownloads(context),
        )
        addAction(
            android.R.drawable.ic_menu_close_clear_cancel,
            labels.cancelAll,
            actions.clearDownloads(context),
        )
        addOpenEntryAction(destination)
    }

    private fun NotificationCompat.Builder.addOpenEntryAction(destination: EntryDownloadEntryIdentity) {
        addAction(
            android.R.drawable.ic_menu_view,
            labels.showEntry,
            actions.openEntry(
                context,
                destination.profileId,
                destination.entryId,
            ),
        )
    }

    private fun notificationBuilder(
        channelId: String,
        block: NotificationCompat.Builder.() -> Unit,
    ): NotificationCompat.Builder = NotificationCompat.Builder(context, channelId).apply(block)

    private fun NotificationCompat.Builder.show(id: Int) {
        publisher.notify(id, build())
    }
}

fun Context.entryDownloadForegroundNotification(): Notification {
    return NotificationCompat.Builder(this, AppNotificationKind.DOWNLOAD_PROGRESS.id)
        .setContentTitle(stringResource(MR.strings.download_notifier_downloader_title))
        .setSmallIcon(android.R.drawable.stat_sys_download)
        .setOngoing(true)
        .build()
}

private fun Context.entryDownloadNotificationLabels() = EntryDownloadNotificationLabels(
    downloader = stringResource(MR.strings.download_notifier_downloader_title),
    unknownError = stringResource(MR.strings.download_notifier_unknown_error),
    paused = stringResource(MR.strings.chapter_paused),
    downloadsPaused = stringResource(MR.strings.download_notifier_download_paused),
    pause = stringResource(MR.strings.action_pause),
    resume = stringResource(MR.strings.action_resume),
    cancelAll = stringResource(MR.strings.action_cancel_all),
    showEntry = stringResource(MR.strings.action_show_manga),
)

private class AndroidEntryDownloadNotificationPublisher(
    private val context: Context,
) : EntryDownloadNotificationPublisher {
    override fun notify(id: Int, notification: Notification) {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            PermissionChecker.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PermissionChecker.PERMISSION_GRANTED
        ) {
            return
        }
        NotificationManagerCompat.from(context).notify(id, notification)
    }

    override fun cancel(id: Int) {
        NotificationManagerCompat.from(context).cancel(id)
    }
}
