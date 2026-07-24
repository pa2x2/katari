package mihon.entry.interactions.download.notification

import android.Manifest
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.PermissionChecker
import eu.kanade.tachiyomi.core.security.SecurityPreferences
import mihon.entry.interactions.EntryDownloadEntryIdentity
import mihon.entry.interactions.EntryDownloadNotificationActions
import mihon.entry.interactions.EntryDownloadNotifications
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.i18n.MR
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

data class EntryDownloadProgressNotification(
    val destination: EntryDownloadEntryIdentity,
    val title: String,
    val text: String?,
    val maximum: Int,
    val current: Int,
    val indeterminate: Boolean = false,
    val hiddenTitle: String? = null,
)

data class EntryDownloadErrorNotification(
    val destination: EntryDownloadEntryIdentity?,
    val title: String,
    val message: String,
)

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

interface EntryDownloadNotificationPresenter {
    fun showProgress(content: EntryDownloadProgressNotification)
    fun showPaused()
    fun onComplete()
    fun showError(content: EntryDownloadErrorNotification)
    fun showWarning(
        reason: String,
        timeout: Long? = null,
        contentIntent: PendingIntent? = null,
    )
}

class AndroidEntryDownloadNotifier(
    private val context: Context,
    private val actions: EntryDownloadNotificationActions = Injekt.get(),
    private val hideNotificationContent: () -> Boolean = {
        Injekt.get<SecurityPreferences>().hideNotificationContent.get()
    },
    private val labels: EntryDownloadNotificationLabels = context.entryDownloadNotificationLabels(),
    private val publisher: EntryDownloadNotificationPublisher = AndroidEntryDownloadNotificationPublisher(context),
) : EntryDownloadNotificationPresenter {
    override fun showProgress(content: EntryDownloadProgressNotification) {
        val hideContent = hideNotificationContent()
        notificationBuilder(EntryDownloadNotifications.CHANNEL_PROGRESS) {
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
        notificationBuilder(EntryDownloadNotifications.CHANNEL_PROGRESS) {
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
        }.show(EntryDownloadNotifications.ID_PROGRESS)
    }

    override fun onComplete() {
        publisher.cancel(EntryDownloadNotifications.ID_PROGRESS)
    }

    override fun showError(content: EntryDownloadErrorNotification) {
        val hideContent = hideNotificationContent()
        notificationBuilder(EntryDownloadNotifications.CHANNEL_ERROR) {
            setContentTitle(content.title.takeUnless { hideContent } ?: labels.downloader)
            setContentText(content.message.takeUnless { hideContent } ?: labels.unknownError)
            setSmallIcon(android.R.drawable.stat_sys_warning)
            setAutoCancel(true)
            setContentIntent(actions.openDownloadManager(context))
            content.destination?.let { addOpenEntryAction(it) }
        }.show(EntryDownloadNotifications.ID_ERROR)
    }

    override fun showWarning(
        reason: String,
        timeout: Long?,
        contentIntent: PendingIntent?,
    ) {
        notificationBuilder(EntryDownloadNotifications.CHANNEL_ERROR) {
            setContentTitle(labels.downloader)
            setStyle(NotificationCompat.BigTextStyle().bigText(reason))
            setSmallIcon(android.R.drawable.stat_sys_warning)
            setAutoCancel(true)
            setContentIntent(contentIntent ?: actions.openDownloadManager(context))
            timeout?.let(::setTimeoutAfter)
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
    return NotificationCompat.Builder(this, EntryDownloadNotifications.CHANNEL_PROGRESS)
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
