package mihon.entry.interactions

import android.Manifest
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.PermissionChecker
import eu.kanade.tachiyomi.source.entry.EntryType

interface EntryDownloadNotificationActions {
    fun openDownloadManager(context: Context): PendingIntent
    fun pauseDownloads(context: Context): PendingIntent
    fun resumeDownloads(context: Context): PendingIntent
    fun clearDownloads(context: Context): PendingIntent
    fun openEntry(context: Context, entryType: EntryType, entryId: Long): PendingIntent
    fun openUrl(context: Context, url: String): PendingIntent
}

object EntryDownloadNotifications {
    const val CHANNEL_PROGRESS = "downloader_progress_channel"
    const val CHANNEL_ERROR = "downloader_error_channel"
    const val ID_MANGA_PROGRESS = -201
    const val ID_MANGA_ERROR = -202
    const val ID_ANIME_PROGRESS = -203
    const val ID_ANIME_ERROR = -204
}

fun Context.entryDownloadNotificationBuilder(
    channelId: String,
    block: (NotificationCompat.Builder.() -> Unit)? = null,
): NotificationCompat.Builder {
    return NotificationCompat.Builder(this, channelId).apply {
        block?.invoke(this)
    }
}

fun Context.notifyEntryDownload(id: Int, notification: Notification) {
    if (
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        PermissionChecker.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS,
        ) != PermissionChecker.PERMISSION_GRANTED
    ) {
        return
    }

    NotificationManagerCompat.from(this).notify(id, notification)
}

fun Context.cancelEntryDownloadNotification(id: Int) {
    NotificationManagerCompat.from(this).cancel(id)
}
