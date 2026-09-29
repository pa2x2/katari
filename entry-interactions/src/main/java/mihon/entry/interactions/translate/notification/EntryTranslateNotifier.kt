package mihon.entry.interactions.translate.notification

import android.app.Notification
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import mihon.entry.interactions.download.EntryDownloadNotificationActions
import mihon.entry.interactions.translate.EntryTranslateNotifications
import mihon.entry.interactions.translate.EntryTranslateWaiting
import mihon.entry.interactions.translate.work.EntryTranslateQueueRunner
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.i18n.*

/**
 * The ongoing notification of background translation, showing the chapter being translated or what it waits for, and
 * the notice left when the queue is paused.
 */
internal class EntryTranslateNotifier(
    private val context: Context,
    private val actions: EntryDownloadNotificationActions,
    private val hideContent: () -> Boolean,
) {
    val notificationId = EntryTranslateNotifications.ID_PROGRESS

    fun notification(active: EntryTranslateQueueRunner.Active?, waiting: EntryTranslateWaiting?): Notification =
        NotificationCompat.Builder(context, EntryTranslateNotifications.CHANNEL_PROGRESS)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(context.stringResource(MR.strings.translate_notifier_title))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(actions.openDownloadManager(context))
            .addAction(
                android.R.drawable.ic_media_pause,
                context.stringResource(MR.strings.action_pause),
                EntryTranslateNotificationReceiver.Action.Pause.pendingIntent(context),
            )
            .addCancelAllAction()
            .apply {
                if (waiting != null) {
                    setContentText(context.stringResource(waiting.text))
                    return@apply
                }
                if (active == null) {
                    setProgress(0, 0, true)
                    return@apply
                }
                if (!hideContent()) setContentText("${active.entry.title} · ${active.chapter.name}")
                val progress = active.progress
                setProgress(progress.total, progress.done, progress.total == 0)
            }
            .build()

    fun update(active: EntryTranslateQueueRunner.Active?, waiting: EntryTranslateWaiting?) {
        notify(notificationId, notification(active, waiting))
    }

    fun showPaused() {
        val notification = NotificationCompat.Builder(context, EntryTranslateNotifications.CHANNEL_PROGRESS)
            .setSmallIcon(android.R.drawable.ic_media_pause)
            .setContentTitle(context.stringResource(MR.strings.translate_notifier_paused))
            .setOnlyAlertOnce(true)
            .setContentIntent(actions.openDownloadManager(context))
            .addAction(
                android.R.drawable.ic_media_play,
                context.stringResource(MR.strings.action_resume),
                EntryTranslateNotificationReceiver.Action.Resume.pendingIntent(context),
            )
            .addCancelAllAction()
            .build()
        notify(EntryTranslateNotifications.ID_PAUSED, notification)
    }

    fun dismissPaused() {
        NotificationManagerCompat.from(context).cancel(EntryTranslateNotifications.ID_PAUSED)
    }

    private fun NotificationCompat.Builder.addCancelAllAction() = addAction(
        android.R.drawable.ic_menu_close_clear_cancel,
        context.stringResource(MR.strings.action_cancel_all),
        EntryTranslateNotificationReceiver.Action.CancelAll.pendingIntent(context),
    )

    private fun notify(id: Int, notification: Notification) {
        try {
            NotificationManagerCompat.from(context).notify(id, notification)
        } catch (_: SecurityException) {
            // Without the notification permission the translation still runs.
        }
    }
}

private val EntryTranslateWaiting.text
    get() = when (this) {
        EntryTranslateWaiting.Charger -> MR.strings.translate_waiting_for_charger
        EntryTranslateWaiting.Wifi -> MR.strings.translate_waiting_for_wifi
    }
