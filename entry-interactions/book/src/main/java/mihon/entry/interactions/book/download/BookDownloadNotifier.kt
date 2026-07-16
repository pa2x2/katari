package mihon.entry.interactions.book.download

import android.content.Context
import androidx.core.app.NotificationCompat
import mihon.entry.interactions.EntryDownloadNotificationActions
import mihon.entry.interactions.EntryDownloadNotifications
import mihon.entry.interactions.book.download.model.BookDownload
import mihon.entry.interactions.book.download.model.BookDownloadFailure
import mihon.entry.interactions.cancelEntryDownloadNotification
import mihon.entry.interactions.entryDownloadNotificationBuilder
import mihon.entry.interactions.notifyEntryDownload
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.i18n.MR
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

internal class BookDownloadNotifier(private val context: Context) {
    private val notificationActions by lazy { Injekt.get<EntryDownloadNotificationActions>() }
    private val progressNotificationBuilder by lazy {
        context.entryDownloadNotificationBuilder(EntryDownloadNotifications.CHANNEL_PROGRESS) {
            setSmallIcon(android.R.drawable.stat_sys_download)
            setAutoCancel(false)
            setOnlyAlertOnce(true)
            setOngoing(true)
        }
    }
    private val errorNotificationBuilder by lazy {
        context.entryDownloadNotificationBuilder(EntryDownloadNotifications.CHANNEL_ERROR) {
            setAutoCancel(true)
            setSmallIcon(android.R.drawable.stat_sys_warning)
        }
    }

    fun onProgressChange(download: BookDownload) {
        with(progressNotificationBuilder) {
            setContentIntent(notificationActions.openDownloadManager(context))
            clearActions()
            addAction(
                android.R.drawable.ic_media_pause,
                context.stringResource(MR.strings.action_pause),
                notificationActions.pauseDownloads(context),
            )
            addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                context.stringResource(MR.strings.action_cancel_all),
                notificationActions.clearDownloads(context),
            )
            setContentTitle(download.entry.title)
            setContentText(download.chapter.name.ifBlank { "Book" })
            setProgress(100, download.progress, download.progress <= 0)
            show(EntryDownloadNotifications.ID_BOOK_PROGRESS)
        }
    }

    fun onPaused() {
        with(progressNotificationBuilder) {
            setContentTitle(context.stringResource(MR.strings.chapter_paused))
            setContentText(context.stringResource(MR.strings.download_notifier_download_paused))
            setSmallIcon(android.R.drawable.ic_media_pause)
            setProgress(0, 0, false)
            setOngoing(false)
            clearActions()
            setContentIntent(notificationActions.openDownloadManager(context))
            addAction(
                android.R.drawable.ic_media_play,
                context.stringResource(MR.strings.action_resume),
                notificationActions.resumeDownloads(context),
            )
            addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                context.stringResource(MR.strings.action_cancel_all),
                notificationActions.clearDownloads(context),
            )
            show(EntryDownloadNotifications.ID_BOOK_PROGRESS)
        }
    }

    fun onComplete() {
        context.cancelEntryDownloadNotification(EntryDownloadNotifications.ID_BOOK_PROGRESS)
    }

    fun onError(download: BookDownload) {
        with(errorNotificationBuilder) {
            setContentTitle(download.entry.title)
            setContentText(download.failure.toReadableMessage(context))
            setContentIntent(notificationActions.openDownloadManager(context))
            show(EntryDownloadNotifications.ID_BOOK_ERROR)
        }
    }

    private fun NotificationCompat.Builder.show(id: Int) {
        context.notifyEntryDownload(id, build())
    }
}

private fun BookDownloadFailure?.toReadableMessage(context: Context): String {
    val failure = this ?: return context.stringResource(MR.strings.download_notifier_unknown_error)
    return failure.message?.takeIf(String::isNotBlank) ?: when (failure.reason) {
        BookDownloadFailure.Reason.SOURCE_NOT_FOUND -> "Source not available"
        BookDownloadFailure.Reason.CONTENT_UNAVAILABLE -> "Book content is not available"
        BookDownloadFailure.Reason.UNSUPPORTED_FORMAT -> "No installed reader supports this book format"
        BookDownloadFailure.Reason.AMBIGUOUS_RESOURCE -> "The book item does not identify one downloadable resource"
        BookDownloadFailure.Reason.STORAGE -> "Unable to store the book download"
        BookDownloadFailure.Reason.INTEGRITY -> "The downloaded book failed integrity verification"
        BookDownloadFailure.Reason.NETWORK -> "Network error while downloading book content"
        BookDownloadFailure.Reason.UNKNOWN -> context.stringResource(MR.strings.download_notifier_unknown_error)
    }
}
