package mihon.app.sync

import android.app.Application
import android.app.Notification
import app.katari.sync.client.status.SyncStatus
import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.util.system.notificationBuilder
import tachiyomi.i18n.*
import tachiyomi.i18n.context.stringResource

/**
 * The notification the first download shows while it runs: how far it got and the server it comes from, opening the
 * Sync page. Only the first download shows one; later syncs take seconds and stay quiet.
 */
@Inject
class SyncFirstDownloadNotification(private val application: Application) {

    /** The notification for [status], or for a download that hasn't reported yet when it is null. */
    fun build(status: SyncStatus?): Notification {
        val percent = (status?.activity as? SyncStatus.Activity.Syncing)?.percent
        val server = (status?.link as? SyncStatus.Link.Linked)?.server?.host
        return application.notificationBuilder(Notifications.CHANNEL_SYNC_PROGRESS) {
            setSmallIcon(R.drawable.ic_katari)
            setContentTitle(application.stringResource(MR.strings.sync_status_downloading_unknown))
            setContentText(
                when {
                    server != null && percent != null ->
                        application.stringResource(MR.strings.sync_first_download_progress, server, percent)
                    else -> server
                },
            )
            setProgress(PERCENT, percent ?: 0, percent == null)
            setOngoing(true)
            setOnlyAlertOnce(true)
            setContentIntent(
                SyncNavigation.pendingIntent(
                    application,
                    SyncNavigation.ACTION_OPEN_SYNC,
                    Notifications.ID_SYNC_FIRST_DOWNLOAD,
                ),
            )
        }.build()
    }

    private companion object {
        const val PERCENT = 100
    }
}
