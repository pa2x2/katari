package app.katari.desktop.migration

import app.katari.desktop.home.HomeDestination
import app.katari.desktop.notification.DesktopNotification
import app.katari.desktop.notification.DesktopNotifications
import app.katari.desktop.notification.NotificationAction
import app.katari.desktop.notification.NotificationProgress
import app.katari.desktop.notification.NotificationResponse
import app.katari.desktop.notification.NotificationUrgency
import app.katari.notification.AppNotificationKind
import app.katari.sourcemigration.session.model.SourceMigrationSessionId
import app.katari.sourcemigration.work.SourceMigrationWorkScheduler
import dev.icerock.moko.resources.StringResource
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import tachiyomi.i18n.MR
import tachiyomi.i18n.action_pause
import tachiyomi.i18n.localized.stringResource
import tachiyomi.i18n.sourceMigration_notification_completeText
import tachiyomi.i18n.sourceMigration_notification_completeTitle
import tachiyomi.i18n.sourceMigration_notification_discoveryProgress
import tachiyomi.i18n.sourceMigration_notification_discoveryStarting
import tachiyomi.i18n.sourceMigration_notification_discoveryTitle
import tachiyomi.i18n.sourceMigration_notification_executionProgress
import tachiyomi.i18n.sourceMigration_notification_executionStarting
import tachiyomi.i18n.sourceMigration_notification_executionTitle
import tachiyomi.i18n.sourceMigration_notification_reviewReadyText
import tachiyomi.i18n.sourceMigration_notification_reviewReadyTitle

/**
 * A migration session's notifications, as Android posts them: the progress of finding replacements and of migrating,
 * which can pause it and opens the session's review, then that the review is ready or the migration done. Nothing is
 * posted for a session whose review shows, since the review shows the same.
 */
@Inject
@SingleIn(AppScope::class)
class DesktopSourceMigrationNotifier(
    private val notifications: DesktopNotifications,
    private val visibility: DesktopSourceMigrationReviewVisibility,
    // The scheduler starts the work that posts these
    private val scheduler: Lazy<SourceMigrationWorkScheduler>,
) {
    fun showDiscoveryProgress(sessionId: SourceMigrationSessionId, completed: Int, total: Int) {
        showProgress(
            sessionId,
            MR.strings.sourceMigration_notification_discoveryTitle,
            if (total > 0) {
                stringResource(MR.strings.sourceMigration_notification_discoveryProgress, completed, total)
            } else {
                stringResource(MR.strings.sourceMigration_notification_discoveryStarting)
            },
            NotificationProgress(completed.coerceAtLeast(0), total.coerceAtLeast(0)),
        ) { scheduler.value.pauseDiscovery(sessionId) }
    }

    fun showExecutionProgress(sessionId: SourceMigrationSessionId, completed: Int, total: Int) {
        showProgress(
            sessionId,
            MR.strings.sourceMigration_notification_executionTitle,
            if (total > 0) {
                stringResource(MR.strings.sourceMigration_notification_executionProgress, completed, total)
            } else {
                stringResource(MR.strings.sourceMigration_notification_executionStarting)
            },
            NotificationProgress(completed.coerceAtLeast(0), total.coerceAtLeast(0)),
        ) { scheduler.value.pauseExecution(sessionId) }
    }

    fun showReviewReady(sessionId: SourceMigrationSessionId, completed: Int) {
        showResult(
            sessionId,
            DesktopNotification(
                kind = AppNotificationKind.SOURCE_MIGRATION_COMPLETE,
                title = stringResource(MR.strings.sourceMigration_notification_reviewReadyTitle),
                body = stringResource(MR.strings.sourceMigration_notification_reviewReadyText, completed),
                onClick = NotificationResponse.Open(HomeDestination.SourceMigrationReview(sessionId)),
            ),
        )
    }

    fun showExecutionComplete(sessionId: SourceMigrationSessionId, migrated: Int, attention: Int) {
        showResult(
            sessionId,
            DesktopNotification(
                kind = AppNotificationKind.SOURCE_MIGRATION_COMPLETE,
                title = stringResource(MR.strings.sourceMigration_notification_completeTitle),
                body = stringResource(MR.strings.sourceMigration_notification_completeText, migrated, attention),
            ),
        )
    }

    fun withdraw(sessionId: SourceMigrationSessionId) {
        notifications.withdraw(progressKey(sessionId))
        notifications.withdraw(resultKey(sessionId))
    }

    private fun showProgress(
        sessionId: SourceMigrationSessionId,
        title: StringResource,
        text: String,
        progress: NotificationProgress,
        pause: suspend () -> Unit,
    ) {
        if (visibility.isShown(sessionId)) {
            withdraw(sessionId)
            return
        }
        notifications.show(
            progressKey(sessionId),
            DesktopNotification(
                kind = AppNotificationKind.SOURCE_MIGRATION_PROGRESS,
                title = stringResource(title),
                body = text,
                progress = progress,
                urgency = NotificationUrgency.Low,
                onClick = NotificationResponse.Open(HomeDestination.SourceMigrationReview(sessionId)),
                actions = listOf(
                    NotificationAction(stringResource(MR.strings.action_pause), NotificationResponse.Run(pause)),
                ),
            ),
        )
    }

    private fun showResult(sessionId: SourceMigrationSessionId, notification: DesktopNotification) {
        withdraw(sessionId)
        if (visibility.isShown(sessionId)) return
        notifications.show(resultKey(sessionId), notification)
    }

    private companion object {
        fun progressKey(sessionId: SourceMigrationSessionId) = "source-migration-progress-${sessionId.value}"

        fun resultKey(sessionId: SourceMigrationSessionId) = "source-migration-result-${sessionId.value}"
    }
}
