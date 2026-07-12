package mihon.entry.interactions.anime.download

import android.content.Context
import androidx.core.app.NotificationCompat
import mihon.entry.interactions.EntryDownloadNotificationActions
import mihon.entry.interactions.EntryDownloadNotifications
import mihon.entry.interactions.anime.download.model.AnimeDownload
import mihon.entry.interactions.anime.download.model.AnimeDownloadFailure
import mihon.entry.interactions.cancelEntryDownloadNotification
import mihon.entry.interactions.entryDownloadNotificationBuilder
import mihon.entry.interactions.notifyEntryDownload
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.i18n.MR
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

internal class AnimeDownloadNotifier(
    private val context: Context,
) {
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

    private fun NotificationCompat.Builder.show(id: Int) {
        context.notifyEntryDownload(id, build())
    }

    fun dismissProgress() {
        context.cancelEntryDownloadNotification(EntryDownloadNotifications.ID_ANIME_PROGRESS)
    }

    fun onProgressChange(download: AnimeDownload) {
        with(progressNotificationBuilder) {
            setContentIntent(notificationActions.openDownloadManager(context))
            clearActions()
            addAction(
                android.R.drawable.ic_media_pause,
                context.stringResource(MR.strings.action_pause),
                notificationActions.pauseDownloads(context),
            )
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

            setContentTitle(download.anime.title)
            setContentText(download.episode.name.ifBlank { "Episode" })
            val progress = download.progress.coerceIn(0, 100)
            setProgress(100, progress, progress <= 0)
            show(EntryDownloadNotifications.ID_ANIME_PROGRESS)
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
            show(EntryDownloadNotifications.ID_ANIME_PROGRESS)
        }
    }

    fun onComplete() {
        dismissProgress()
    }

    fun onError(download: AnimeDownload) {
        with(errorNotificationBuilder) {
            setContentTitle(download.anime.title)
            setContentText(download.failure.toReadableMessage(context))
            setContentIntent(notificationActions.openDownloadManager(context))
            show(EntryDownloadNotifications.ID_ANIME_ERROR)
        }
    }
}

private fun AnimeDownloadFailure?.toReadableMessage(context: Context): String {
    val failure = this ?: return context.stringResource(MR.strings.download_notifier_unknown_error)
    return failure.message?.takeIf { it.isNotBlank() } ?: when (failure.reason) {
        AnimeDownloadFailure.Reason.SOURCE_NOT_FOUND -> "Source not available"
        AnimeDownloadFailure.Reason.EPISODE_NOT_FOUND -> "Episode not found"
        AnimeDownloadFailure.Reason.PREFERENCES_NOT_SUPPORTED -> "Selected download options are not supported"
        AnimeDownloadFailure.Reason.DUB_NOT_AVAILABLE -> "Selected dub is not available"
        AnimeDownloadFailure.Reason.STREAM_NOT_AVAILABLE -> "Selected stream is not available"
        AnimeDownloadFailure.Reason.SUBTITLE_NOT_AVAILABLE -> "Selected subtitle is not available"
        AnimeDownloadFailure.Reason.QUALITY_NOT_AVAILABLE -> "Selected quality is not available"
        AnimeDownloadFailure.Reason.STREAM_EXPIRED -> "Stream URL expired"
        AnimeDownloadFailure.Reason.UNSUPPORTED_STREAM -> "Source only provides unsupported stream format for download"
        AnimeDownloadFailure.Reason.INSUFFICIENT_STORAGE -> "Insufficient storage space"
        AnimeDownloadFailure.Reason.NETWORK -> "Network error while resolving download stream"
        AnimeDownloadFailure.Reason.UNKNOWN -> context.stringResource(MR.strings.download_notifier_unknown_error)
    }
}
