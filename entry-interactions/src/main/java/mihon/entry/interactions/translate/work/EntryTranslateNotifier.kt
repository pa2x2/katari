package mihon.entry.interactions.translate.work

import android.app.Notification
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import mihon.entry.interactions.download.EntryDownloadNotificationActions
import mihon.entry.interactions.translate.EntryTranslateNotifications
import mihon.entry.interactions.translate.EntryTranslateWaiting
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.i18n.*

/** The ongoing notification of background translation, showing the chapter being translated or what it waits for. */
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
        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification(active, waiting))
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
