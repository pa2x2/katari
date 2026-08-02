package eu.kanade.tachiyomi.entry

import android.app.PendingIntent
import android.content.Context
import eu.kanade.tachiyomi.data.notification.NotificationHandler
import eu.kanade.tachiyomi.data.notification.NotificationReceiver
import mihon.entry.interactions.download.EntryDownloadNotificationActions

class AppEntryDownloadNotificationActions : EntryDownloadNotificationActions {
    override fun openDownloadManager(context: Context): PendingIntent {
        return NotificationHandler.openDownloadManagerPendingActivity(context)
    }

    override fun pauseDownloads(context: Context): PendingIntent {
        return NotificationReceiver.pauseDownloadsPendingBroadcast(context)
    }

    override fun resumeDownloads(context: Context): PendingIntent {
        return NotificationReceiver.resumeDownloadsPendingBroadcast(context)
    }

    override fun clearDownloads(context: Context): PendingIntent {
        return NotificationReceiver.clearDownloadsPendingBroadcast(context)
    }

    override fun openEntry(context: Context, profileId: Long, entryId: Long): PendingIntent {
        return NotificationReceiver.openEntryPendingActivity(context, profileId, entryId)
    }

    override fun openUrl(context: Context, url: String): PendingIntent {
        return NotificationHandler.openUrl(context, url)
    }
}
