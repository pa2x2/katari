package mihon.entry.interactions.translate

import android.app.Notification
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import mihon.entry.interactions.download.EntryDownloadNotificationActions
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.i18n.*

/** The ongoing notification of background translation, showing the chapter being translated. */
internal class EntryTranslateNotifier(
    private val context: Context,
    private val actions: EntryDownloadNotificationActions,
    private val hideContent: () -> Boolean,
) {
    val notificationId = EntryTranslateNotifications.ID_PROGRESS

    fun notification(active: EntryTranslateQueueRunner.Active?): Notification =
        NotificationCompat.Builder(context, EntryTranslateNotifications.CHANNEL_PROGRESS)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(context.stringResource(MR.strings.translate_notifier_title))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(actions.openDownloadManager(context))
            .apply {
                if (active == null) {
                    setProgress(0, 0, true)
                    return@apply
                }
                if (!hideContent()) setContentText("${active.entry.title} · ${active.chapter.name}")
                val progress = active.progress
                setProgress(progress.total, progress.done, progress.total == 0)
            }
            .build()

    fun update(active: EntryTranslateQueueRunner.Active?) {
        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification(active))
        } catch (_: SecurityException) {
            // Without the notification permission the translation still runs.
        }
    }
}
