package mihon.desktop.backup

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.core.security.SecurityPreferences
import eu.kanade.tachiyomi.data.backup.create.BackupCreationProgress
import eu.kanade.tachiyomi.data.backup.restore.BackupRestoreReporter
import mihon.core.platform.PlatformContext
import mihon.core.platform.link.LinkOpener
import mihon.desktop.notification.DesktopNotification
import mihon.desktop.notification.DesktopNotifications
import mihon.desktop.notification.NotificationAction
import mihon.desktop.notification.NotificationProgress
import mihon.desktop.notification.NotificationResponse
import mihon.desktop.notification.NotificationUrgency
import tachiyomi.domain.backup.service.BackupRestoreRunner
import tachiyomi.domain.storage.DocumentUri
import tachiyomi.i18n.MR
import tachiyomi.i18n.action_cancel
import tachiyomi.i18n.action_show_errors
import tachiyomi.i18n.backup_created
import tachiyomi.i18n.creating_backup
import tachiyomi.i18n.creating_backup_entries_progress
import tachiyomi.i18n.creating_backup_error
import tachiyomi.i18n.creating_backup_saving
import tachiyomi.i18n.library_sync_complete
import tachiyomi.i18n.localized.pluralStringResource
import tachiyomi.i18n.localized.stringResource
import tachiyomi.i18n.restore_completed
import tachiyomi.i18n.restore_completed_message
import tachiyomi.i18n.restore_completed_profiles
import tachiyomi.i18n.restore_duration
import tachiyomi.i18n.restoring_backup
import tachiyomi.i18n.restoring_backup_error
import tachiyomi.i18n.syncing_library
import java.io.File
import java.net.URI
import java.net.URISyntaxException
import java.nio.file.Path
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource

/**
 * The notifications of backups and restores, as Android posts them: the progress while one runs, replaced in place,
 * and how it ended. A restore's progress can cancel it, and one that ended with errors opens their list. Android's
 * share action for a created backup stays there: desktop shares nothing yet.
 */
@Inject
@SingleIn(AppScope::class)
class DesktopBackupNotifier(
    private val context: PlatformContext,
    private val notifications: DesktopNotifications,
    private val securityPreferences: SecurityPreferences,
    private val linkOpener: LinkOpener,
    // The runner restores through this
    private val restoreRunner: Lazy<BackupRestoreRunner>,
) : BackupRestoreReporter {
    private var lastEntriesProgress: TimeSource.Monotonic.ValueTimeMark? = null

    @Synchronized
    fun showBackupProgress(progress: BackupCreationProgress) {
        if (progress is BackupCreationProgress.Entries && progress.backedUp in 1..<progress.total) {
            val last = lastEntriesProgress
            if (last != null && last.elapsedNow() < PROGRESS_INTERVAL) return
            lastEntriesProgress = TimeSource.Monotonic.markNow()
        }
        val (text, shownProgress) = when (progress) {
            BackupCreationProgress.Preparing -> null to NotificationProgress(0, 0)
            is BackupCreationProgress.Entries ->
                stringResource(MR.strings.creating_backup_entries_progress, progress.backedUp, progress.total) to
                    NotificationProgress(progress.backedUp, progress.total)
            BackupCreationProgress.Saving ->
                stringResource(MR.strings.creating_backup_saving) to NotificationProgress(0, 0)
        }
        notifications.show(
            BACKUP_PROGRESS,
            DesktopNotification(
                title = stringResource(MR.strings.creating_backup),
                body = text,
                progress = shownProgress,
                urgency = NotificationUrgency.Low,
            ),
        )
    }

    fun withdrawBackupProgress() {
        notifications.withdraw(BACKUP_PROGRESS)
    }

    fun showBackupComplete(location: DocumentUri) {
        withdrawBackupProgress()
        notifications.show(
            BACKUP_COMPLETE,
            DesktopNotification(title = stringResource(MR.strings.backup_created), body = displayable(location)),
        )
    }

    fun showBackupError(error: String?) {
        withdrawBackupProgress()
        notifications.show(
            BACKUP_COMPLETE,
            DesktopNotification(title = stringResource(MR.strings.creating_backup_error), body = error),
        )
    }

    override fun onRestoreProgress(content: String, progress: Int, maxAmount: Int, sync: Boolean) {
        notifications.show(
            RESTORE_PROGRESS,
            DesktopNotification(
                title = stringResource(if (sync) MR.strings.syncing_library else MR.strings.restoring_backup),
                body = content.takeUnless { securityPreferences.hideNotificationContent.get() },
                progress = NotificationProgress(progress, maxAmount),
                urgency = NotificationUrgency.Low,
                actions = listOf(
                    NotificationAction(
                        stringResource(MR.strings.action_cancel),
                        NotificationResponse.Run { restoreRunner.value.stop() },
                    ),
                ),
            ),
        )
    }

    override fun onRestoreComplete(
        time: Long,
        errorCount: Int,
        path: String?,
        file: String?,
        sync: Boolean,
        restoredProfileNames: List<String>,
    ) {
        withdrawRestoreProgress()
        val duration = time.milliseconds
        val timeString = stringResource(
            MR.strings.restore_duration,
            duration.inWholeMinutes,
            duration.inWholeSeconds - duration.inWholeMinutes * 60,
        )
        val message = pluralStringResource(MR.plurals.restore_completed_message, errorCount, timeString, errorCount)
        val profiles = restoredProfileNames.takeIf { it.isNotEmpty() }
            ?.let { stringResource(MR.strings.restore_completed_profiles, it.joinToString()) }
        val errorLog = if (errorCount > 0 && !path.isNullOrEmpty() && !file.isNullOrEmpty()) {
            val uri = File(path, file).toURI().toString()
            NotificationResponse.Run { linkOpener.open(context, uri) }
        } else {
            null
        }
        notifications.show(
            RESTORE_COMPLETE,
            DesktopNotification(
                title = stringResource(if (sync) MR.strings.library_sync_complete else MR.strings.restore_completed),
                body = listOfNotNull(message, profiles).joinToString("\n"),
                onClick = errorLog,
                actions = listOfNotNull(
                    errorLog?.let { NotificationAction(stringResource(MR.strings.action_show_errors), it) },
                ),
            ),
        )
    }

    fun showRestoreError(error: String?) {
        withdrawRestoreProgress()
        notifications.show(
            RESTORE_COMPLETE,
            DesktopNotification(title = stringResource(MR.strings.restoring_backup_error), body = error),
        )
    }

    fun withdrawRestoreProgress() {
        notifications.withdraw(RESTORE_PROGRESS)
    }

    private fun displayable(location: DocumentUri): String = try {
        Path.of(URI(location.value)).toString()
    } catch (_: URISyntaxException) {
        location.value
    } catch (_: IllegalArgumentException) {
        location.value
    }

    private companion object {
        const val BACKUP_PROGRESS = "backup-progress"
        const val BACKUP_COMPLETE = "backup-complete"
        const val RESTORE_PROGRESS = "restore-progress"
        const val RESTORE_COMPLETE = "restore-complete"

        // A count for each entry would call the notification server for each; a few a second follow it as well
        val PROGRESS_INTERVAL = 500.milliseconds
    }
}
