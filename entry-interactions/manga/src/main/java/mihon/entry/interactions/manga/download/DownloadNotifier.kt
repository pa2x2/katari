package mihon.entry.interactions.manga.download

import android.app.PendingIntent
import android.content.Context
import androidx.core.app.NotificationCompat
import eu.kanade.tachiyomi.core.security.SecurityPreferences
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.util.lang.chop
import kotlinx.coroutines.runBlocking
import mihon.entry.interactions.EntryDownloadNotificationActions
import mihon.entry.interactions.EntryDownloadNotifications
import mihon.entry.interactions.cancelEntryDownloadNotification
import mihon.entry.interactions.entryDownloadNotificationBuilder
import mihon.entry.interactions.manga.download.model.MangaDownload
import mihon.entry.interactions.notifyEntryDownload
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.domain.entry.interactor.GetMergedEntry
import tachiyomi.i18n.MR
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy
import java.util.regex.Pattern

/**
 * DownloadNotifier is used to show notifications when downloading one or multiple chapters.
 *
 * @param context context of application
 */
internal class DownloadNotifier(private val context: Context) {

    private val preferences: SecurityPreferences by injectLazy()
    private val getMergedEntry by lazy { Injekt.get<GetMergedEntry>() }
    private val notificationActions by lazy { Injekt.get<EntryDownloadNotificationActions>() }

    private val progressNotificationBuilder by lazy {
        context.entryDownloadNotificationBuilder(EntryDownloadNotifications.CHANNEL_PROGRESS) {
            setAutoCancel(false)
            setOnlyAlertOnce(true)
        }
    }

    private val errorNotificationBuilder by lazy {
        context.entryDownloadNotificationBuilder(EntryDownloadNotifications.CHANNEL_ERROR) {
            setAutoCancel(false)
        }
    }

    /**
     * Status of download. Used for correct notification icon.
     */
    private var isDownloading = false

    /**
     * Shows a notification from this builder.
     *
     * @param id the id of the notification.
     */
    private fun NotificationCompat.Builder.show(id: Int) {
        context.notifyEntryDownload(id, build())
    }

    /**
     * Dismiss the downloader's notification. Downloader error notifications use a different id, so
     * those can only be dismissed by the user.
     */
    fun dismissProgress() {
        context.cancelEntryDownloadNotification(EntryDownloadNotifications.ID_MANGA_PROGRESS)
    }

    /**
     * Called when download progress changes.
     *
     * @param download download object containing download information.
     */
    fun onProgressChange(download: MangaDownload) {
        with(progressNotificationBuilder) {
            if (!isDownloading) {
                setSmallIcon(android.R.drawable.stat_sys_download)
                clearActions()
                // Open download manager when clicked
                setContentIntent(notificationActions.openDownloadManager(context))
                isDownloading = true
                // Pause action
                addAction(
                    android.R.drawable.ic_media_pause,
                    context.stringResource(MR.strings.action_pause),
                    notificationActions.pauseDownloads(context),
                )
                addAction(
                    android.R.drawable.ic_menu_view,
                    context.stringResource(MR.strings.action_show_manga),
                    notificationActions.openEntry(context, EntryType.MANGA, getVisibleMangaId(download.entry.id)),
                )
            }

            val downloadingProgressText = context.stringResource(
                MR.strings.chapter_downloading_progress,
                download.downloadedImages,
                download.pages!!.size,
            )

            if (preferences.hideNotificationContent.get()) {
                setContentTitle(downloadingProgressText)
                setContentText(null)
            } else {
                val title = download.entry.title.chop(15)
                val quotedTitle = Pattern.quote(title)
                val chapter = download.chapter.name.replaceFirst(
                    "$quotedTitle[\\s]*[-]*[\\s]*".toRegex(RegexOption.IGNORE_CASE),
                    "",
                )
                setContentTitle("$title - $chapter".chop(30))
                setContentText(downloadingProgressText)
            }

            setProgress(download.pages!!.size, download.downloadedImages, false)
            setOngoing(true)

            show(EntryDownloadNotifications.ID_MANGA_PROGRESS)
        }
    }

    /**
     * Show notification when download is paused.
     */
    fun onPaused() {
        with(progressNotificationBuilder) {
            setContentTitle(context.stringResource(MR.strings.chapter_paused))
            setContentText(context.stringResource(MR.strings.download_notifier_download_paused))
            setSmallIcon(android.R.drawable.ic_media_pause)
            setProgress(0, 0, false)
            setOngoing(false)
            clearActions()
            // Open download manager when clicked
            setContentIntent(notificationActions.openDownloadManager(context))
            // Resume action
            addAction(
                android.R.drawable.ic_media_play,
                context.stringResource(MR.strings.action_resume),
                notificationActions.resumeDownloads(context),
            )
            // Clear action
            addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                context.stringResource(MR.strings.action_cancel_all),
                notificationActions.clearDownloads(context),
            )

            show(EntryDownloadNotifications.ID_MANGA_PROGRESS)
        }

        // Reset initial values
        isDownloading = false
    }

    /**
     * Resets the state once downloads are completed.
     */
    fun onComplete() {
        dismissProgress()

        // Reset states to default
        isDownloading = false
    }

    /**
     * Called when the downloader receives a warning.
     *
     * @param reason the text to show.
     * @param timeout duration after which to automatically dismiss the notification.
     * @param mangaId the id of the entry being warned about
     * Only works on Android 8+.
     */
    fun onWarning(reason: String, timeout: Long? = null, contentIntent: PendingIntent? = null, mangaId: Long? = null) {
        with(errorNotificationBuilder) {
            setContentTitle(context.stringResource(MR.strings.download_notifier_downloader_title))
            setStyle(NotificationCompat.BigTextStyle().bigText(reason))
            setSmallIcon(android.R.drawable.stat_sys_warning)
            setAutoCancel(true)
            clearActions()
            setContentIntent(notificationActions.openDownloadManager(context))
            if (mangaId != null) {
                addAction(
                    android.R.drawable.ic_menu_view,
                    context.stringResource(MR.strings.action_show_manga),
                    notificationActions.openEntry(context, EntryType.MANGA, getVisibleMangaId(mangaId)),
                )
            }
            setProgress(0, 0, false)
            timeout?.let { setTimeoutAfter(it) }
            contentIntent?.let { setContentIntent(it) }

            show(EntryDownloadNotifications.ID_MANGA_ERROR)
        }

        // Reset download information
        isDownloading = false
    }

    /**
     * Called when the downloader receives an error. It's shown as a separate notification to avoid
     * being overwritten.
     *
     * @param error string containing error information.
     * @param chapter string containing chapter title.
     * @param mangaId the id of the entry that the error occurred on
     */
    fun onError(error: String? = null, chapter: String? = null, mangaTitle: String? = null, mangaId: Long? = null) {
        // Create notification
        with(errorNotificationBuilder) {
            setContentTitle(
                mangaTitle?.plus(": $chapter") ?: context.stringResource(MR.strings.download_notifier_downloader_title),
            )
            setContentText(error ?: context.stringResource(MR.strings.download_notifier_unknown_error))
            setSmallIcon(android.R.drawable.stat_sys_warning)
            clearActions()
            setContentIntent(notificationActions.openDownloadManager(context))
            if (mangaId != null) {
                addAction(
                    android.R.drawable.ic_menu_view,
                    context.stringResource(MR.strings.action_show_manga),
                    notificationActions.openEntry(context, EntryType.MANGA, getVisibleMangaId(mangaId)),
                )
            }
            setProgress(0, 0, false)

            show(EntryDownloadNotifications.ID_MANGA_ERROR)
        }

        // Reset download information
        isDownloading = false
    }

    private fun getVisibleMangaId(mangaId: Long): Long {
        return runBlocking { getMergedEntry.awaitVisibleTargetId(mangaId) }
    }
}
