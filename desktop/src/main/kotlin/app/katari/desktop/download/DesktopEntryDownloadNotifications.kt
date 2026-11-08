package app.katari.desktop.download

import app.katari.core.platform.PlatformContext
import app.katari.core.platform.link.LinkOpener
import app.katari.desktop.home.HomeDestination
import app.katari.desktop.notification.DesktopNotification
import app.katari.desktop.notification.DesktopNotifications
import app.katari.desktop.notification.NotificationAction
import app.katari.desktop.notification.NotificationProgress
import app.katari.desktop.notification.NotificationResponse
import app.katari.desktop.notification.NotificationUrgency
import app.katari.entry.interactions.download.EntryDownloadEntryIdentity
import app.katari.entry.interactions.download.EntryDownloadErrorNotification
import app.katari.entry.interactions.download.EntryDownloadNotificationPresenter
import app.katari.entry.interactions.download.EntryDownloadProgressNotification
import app.katari.entry.interactions.download.EntryDownloadRuntimeFeature
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.core.security.SecurityPreferences
import tachiyomi.i18n.MR
import tachiyomi.i18n.action_cancel_all
import tachiyomi.i18n.action_pause
import tachiyomi.i18n.action_resume
import tachiyomi.i18n.action_show_manga
import tachiyomi.i18n.chapter_paused
import tachiyomi.i18n.download_notifier_download_paused
import tachiyomi.i18n.download_notifier_downloader_title
import tachiyomi.i18n.download_notifier_unknown_error
import tachiyomi.i18n.localized.stringResource
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource

/**
 * The download queue's notifications, as Android posts them: the progress, replaced in place and opening the queue,
 * with Pause, Cancel all and Show entry; the paused queue, with Resume and Cancel all; and the failures and warnings of
 * single downloads. Titles and texts stay out where the user hides notification content.
 */
@Inject
@SingleIn(AppScope::class)
class DesktopEntryDownloadNotifications(
    private val context: PlatformContext,
    private val notifications: DesktopNotifications,
    private val securityPreferences: SecurityPreferences,
    private val linkOpener: LinkOpener,
    // The runtime's queue is what these notifications show
    private val downloads: Lazy<EntryDownloadRuntimeFeature>,
) : EntryDownloadNotificationPresenter {
    // False until the first progress after a pause or completion, which then takes the paused notification away
    private var showingProgress = false
    private var lastProgressShown: TimeSource.Monotonic.ValueTimeMark? = null
    private var lastProgress: EntryDownloadProgressNotification? = null

    // The queue reports every page or segment, and its state again for changes the notification doesn't show; repeats
    // and updates closer together than the eye follows are skipped, except the first of each download
    @Synchronized
    override fun showProgress(content: EntryDownloadProgressNotification) {
        if (!showingProgress) {
            notifications.withdraw(PAUSED)
            showingProgress = true
        }
        val last = lastProgressShown
        val previous = lastProgress
        if (content == previous) return
        if (content.title == previous?.title && last != null && last.elapsedNow() < PROGRESS_INTERVAL) return
        lastProgressShown = TimeSource.Monotonic.markNow()
        lastProgress = content
        val hideContent = securityPreferences.hideNotificationContent.get()
        notifications.show(
            PROGRESS,
            DesktopNotification(
                title = if (hideContent) content.hiddenTitle ?: downloader() else content.title,
                body = content.text.takeUnless { hideContent },
                progress = if (content.indeterminate) {
                    NotificationProgress(0, 0)
                } else {
                    NotificationProgress(content.current.coerceIn(0, content.maximum), content.maximum)
                },
                urgency = NotificationUrgency.Low,
                onClick = NotificationResponse.Open(HomeDestination.DownloadQueue),
                actions = listOf(
                    NotificationAction(
                        stringResource(MR.strings.action_pause),
                        NotificationResponse.Run { downloads.value.pause() },
                    ),
                    cancelAll(),
                    showEntry(content.destination),
                ),
            ),
        )
    }

    @Synchronized
    override fun showPaused() {
        endProgress()
        notifications.show(
            PAUSED,
            DesktopNotification(
                title = stringResource(MR.strings.chapter_paused),
                body = stringResource(MR.strings.download_notifier_download_paused),
                onClick = NotificationResponse.Open(HomeDestination.DownloadQueue),
                actions = listOf(
                    NotificationAction(
                        stringResource(MR.strings.action_resume),
                        NotificationResponse.Run { downloads.value.start() },
                    ),
                    cancelAll(),
                ),
            ),
        )
    }

    @Synchronized
    override fun onComplete() {
        endProgress()
        notifications.withdraw(PAUSED)
    }

    override fun showError(content: EntryDownloadErrorNotification) {
        val hideContent = securityPreferences.hideNotificationContent.get()
        notifications.show(
            ERROR,
            DesktopNotification(
                title = content.title.takeUnless { hideContent } ?: downloader(),
                body = content.message.takeUnless { hideContent }
                    ?: stringResource(MR.strings.download_notifier_unknown_error),
                onClick = NotificationResponse.Open(HomeDestination.DownloadQueue),
                actions = listOfNotNull(content.destination?.let(::showEntry)),
            ),
        )
    }

    override fun showWarning(reason: String, timeoutMillis: Long?, helpUrl: String?) {
        notifications.show(
            ERROR,
            DesktopNotification(
                title = downloader(),
                body = reason,
                onClick = helpUrl?.let { url -> NotificationResponse.Run { linkOpener.open(context, url) } }
                    ?: NotificationResponse.Open(HomeDestination.DownloadQueue),
                timeout = timeoutMillis?.milliseconds,
            ),
        )
    }

    private fun endProgress() {
        showingProgress = false
        lastProgressShown = null
        lastProgress = null
        notifications.withdraw(PROGRESS)
    }

    private fun downloader() = stringResource(MR.strings.download_notifier_downloader_title)

    private fun cancelAll() = NotificationAction(
        stringResource(MR.strings.action_cancel_all),
        NotificationResponse.Run { downloads.value.clearQueue() },
    )

    private fun showEntry(destination: EntryDownloadEntryIdentity) = NotificationAction(
        stringResource(MR.strings.action_show_manga),
        NotificationResponse.Open(HomeDestination.Entry(destination.profileId, destination.entryId)),
    )

    private companion object {
        const val PROGRESS = "download-progress"
        const val PAUSED = "download-paused"
        const val ERROR = "download-error"
        val PROGRESS_INTERVAL = 500.milliseconds
    }
}
