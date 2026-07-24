package eu.kanade.tachiyomi.entry

import android.app.PendingIntent
import android.content.Context
import eu.kanade.tachiyomi.data.cache.MangaPageCache
import eu.kanade.tachiyomi.data.notification.NotificationHandler
import eu.kanade.tachiyomi.data.notification.NotificationReceiver
import mihon.entry.interactions.EntryDownloadNotificationActions
import mihon.entry.interactions.EntryPageImageCache
import java.io.File

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

class AppMangaPageImageCache(
    private val pageCache: MangaPageCache,
) : EntryPageImageCache {
    override val readableSize: String
        get() = pageCache.readableSize

    override fun isImageInCache(imageUrl: String): Boolean {
        return pageCache.isImageInCache(imageUrl)
    }

    override fun getImageFile(imageUrl: String): File {
        return pageCache.getImageFile(imageUrl)
    }

    override fun clear(): Int {
        return pageCache.clear()
    }
}
