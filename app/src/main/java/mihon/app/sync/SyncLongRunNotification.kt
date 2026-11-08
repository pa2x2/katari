package mihon.app.sync

import android.app.Application
import android.app.Notification
import app.katari.sync.client.status.SyncStatus
import app.katari.sync.ui.notice.SyncLongRunNotice
import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.util.system.notificationBuilder
import tachiyomi.i18n.*
import tachiyomi.i18n.context.stringResource

/**
 * The notification a long run shows while it goes, opening the Sync page. Only long runs show one; ordinary
 * syncs take seconds and stay quiet.
 */
@Inject
class SyncLongRunNotification(private val application: Application) {

    /** The notification for [status], or for a long run that hasn't reported yet when it is null. */
    fun build(status: SyncStatus?): Notification {
        val (title, server, percent) = SyncLongRunNotice.of(status)
        return application.notificationBuilder(Notifications.CHANNEL_SYNC_PROGRESS) {
            setSmallIcon(R.drawable.ic_katari)
            setContentTitle(application.stringResource(title))
            setContentText(
                when {
                    server != null && percent != null ->
                        application.stringResource(MR.strings.sync_long_run_progress, server, percent)
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
                    Notifications.ID_SYNC_LONG_RUN,
                ),
            )
        }.build()
    }

    private companion object {
        const val PERCENT = 100
    }
}
