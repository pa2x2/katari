package mihon.entry.interactions

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import eu.kanade.tachiyomi.source.entry.EntryType

interface EntryDownloadNotificationActions {
    fun openDownloadManager(context: Context): PendingIntent
    fun pauseDownloads(context: Context): PendingIntent
    fun resumeDownloads(context: Context): PendingIntent
    fun clearDownloads(context: Context): PendingIntent
    fun openEntry(context: Context, entryType: EntryType, entryId: Long): PendingIntent
    fun openUrl(context: Context, url: String): PendingIntent
}

interface EntryDownloadForegroundNotificationProvider {
    val notificationId: Int
    fun notification(): Notification
}

object EntryDownloadNotifications {
    const val CHANNEL_PROGRESS = "downloader_progress_channel"
    const val CHANNEL_ERROR = "downloader_error_channel"
    const val ID_PROGRESS = -201
    const val ID_ERROR = -202
}
