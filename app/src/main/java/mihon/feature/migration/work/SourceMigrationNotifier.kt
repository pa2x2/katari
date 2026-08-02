package mihon.feature.migration.work

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.util.system.cancelNotification
import eu.kanade.tachiyomi.util.system.notificationBuilder
import eu.kanade.tachiyomi.util.system.notify
import mihon.feature.migration.session.model.SourceMigrationSessionId
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.i18n.MR

class SourceMigrationNotifier(private val context: Context) {

    fun discoveryProgressNotification(
        sessionId: SourceMigrationSessionId,
        completed: Int,
        total: Int,
    ): Notification {
        return context.notificationBuilder(Notifications.CHANNEL_SOURCE_MIGRATION_PROGRESS) {
            setContentTitle(context.stringResource(MR.strings.sourceMigration_notification_discoveryTitle))
            setContentText(
                if (total > 0) {
                    context.stringResource(
                        MR.strings.sourceMigration_notification_discoveryProgress,
                        completed,
                        total,
                    )
                } else {
                    context.stringResource(MR.strings.sourceMigration_notification_discoveryStarting)
                },
            )
            setSmallIcon(R.drawable.ic_refresh_24dp)
            setOngoing(true)
            setOnlyAlertOnce(true)
            setProgress(total.coerceAtLeast(0), completed.coerceAtLeast(0), total == 0)
            setContentIntent(openSessionIntent(sessionId))
            addAction(
                R.drawable.ic_pause_24dp,
                context.stringResource(MR.strings.action_pause),
                SourceMigrationActionReceiver.pauseDiscoveryIntent(context, sessionId),
            )
        }.build()
    }

    fun showDiscoveryProgress(sessionId: SourceMigrationSessionId, completed: Int, total: Int) {
        context.notify(notificationId(sessionId), discoveryProgressNotification(sessionId, completed, total))
    }

    fun showReviewReady(sessionId: SourceMigrationSessionId, completed: Int) {
        cancel(sessionId)
        context.notify(
            resultNotificationId(sessionId),
            Notifications.CHANNEL_SOURCE_MIGRATION_COMPLETE,
        ) {
            setContentTitle(context.stringResource(MR.strings.sourceMigration_notification_reviewReadyTitle))
            setContentText(
                context.stringResource(MR.strings.sourceMigration_notification_reviewReadyText, completed),
            )
            setSmallIcon(R.drawable.ic_refresh_24dp)
            setAutoCancel(true)
            setContentIntent(openSessionIntent(sessionId))
        }
    }

    fun cancel(sessionId: SourceMigrationSessionId) {
        context.cancelNotification(notificationId(sessionId))
    }

    fun notificationId(sessionId: SourceMigrationSessionId): Int {
        return NOTIFICATION_ID_BASE + (sessionId.value.hashCode() and NOTIFICATION_ID_MASK)
    }

    private fun resultNotificationId(sessionId: SourceMigrationSessionId): Int {
        return NOTIFICATION_RESULT_ID_BASE + (sessionId.value.hashCode() and NOTIFICATION_ID_MASK)
    }

    private fun openSessionIntent(sessionId: SourceMigrationSessionId): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .putExtra(EXTRA_SOURCE_MIGRATION_SESSION_ID, sessionId.value)
            .setAction(ACTION_OPEN_SOURCE_MIGRATION_SESSION)
        return PendingIntent.getActivity(
            context,
            notificationId(sessionId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        const val ACTION_OPEN_SOURCE_MIGRATION_SESSION = "app.katari.action.OPEN_SOURCE_MIGRATION_SESSION"
        const val EXTRA_SOURCE_MIGRATION_SESSION_ID = "source_migration_session_id"

        private const val NOTIFICATION_ID_BASE = 200_000_000
        private const val NOTIFICATION_RESULT_ID_BASE = 500_000_000
        private const val NOTIFICATION_ID_MASK = 0x07FFFFFF
    }
}
