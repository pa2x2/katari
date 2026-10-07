package eu.kanade.tachiyomi.data.backup

import android.app.Notification
import android.content.Context
import android.graphics.BitmapFactory
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.core.security.SecurityPreferences
import eu.kanade.tachiyomi.data.backup.create.BackupCreationProgress
import eu.kanade.tachiyomi.data.notification.NotificationReceiver
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.util.storage.getUriCompat
import eu.kanade.tachiyomi.util.system.cancelNotification
import eu.kanade.tachiyomi.util.system.notificationBuilder
import eu.kanade.tachiyomi.util.system.notify
import tachiyomi.core.common.i18n.pluralStringResource
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.core.common.storage.displayablePath
import tachiyomi.i18n.*
import uy.kohesive.injekt.injectLazy
import java.io.File
import kotlin.time.Duration.Companion.milliseconds

class BackupNotifier(private val context: Context) {

    private val preferences: SecurityPreferences by injectLazy()

    private val progressNotificationBuilder = context.notificationBuilder(
        Notifications.CHANNEL_BACKUP_RESTORE_PROGRESS,
    ) {
        setLargeIcon(BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher))
        setSmallIcon(R.drawable.ic_katari)
        setAutoCancel(false)
        setOngoing(true)
        setOnlyAlertOnce(true)
    }

    private val completeNotificationBuilder = context.notificationBuilder(
        Notifications.CHANNEL_BACKUP_RESTORE_COMPLETE,
    ) {
        setLargeIcon(BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher))
        setSmallIcon(R.drawable.ic_katari)
        setAutoCancel(false)
    }

    private fun NotificationCompat.Builder.show(id: Int) {
        context.notify(id, build())
    }

    private var lastEntriesProgressAt = 0L

    fun backupProgressNotification(progress: BackupCreationProgress): Notification {
        return with(progressNotificationBuilder) {
            setContentTitle(context.stringResource(MR.strings.creating_backup))
            when (progress) {
                BackupCreationProgress.Preparing -> {
                    setContentText(null)
                    setProgress(0, 0, true)
                }
                is BackupCreationProgress.Entries -> {
                    setContentText(
                        context.stringResource(
                            MR.strings.creating_backup_entries_progress,
                            progress.backedUp,
                            progress.total,
                        ),
                    )
                    setProgress(progress.total, progress.backedUp, false)
                }
                BackupCreationProgress.Saving -> {
                    setContentText(context.stringResource(MR.strings.creating_backup_saving))
                    setProgress(0, 0, true)
                }
            }
            build()
        }
    }

    fun showBackupProgress(progress: BackupCreationProgress) {
        if (progress is BackupCreationProgress.Entries && progress.backedUp in 1..<progress.total) {
            // The system drops updates from apps posting more than about 5 per second, which could swallow the
            // following stage change, so intermediate counts are spaced out.
            val now = SystemClock.elapsedRealtime()
            if (now - lastEntriesProgressAt < ENTRIES_PROGRESS_INTERVAL_MS) return
            lastEntriesProgressAt = now
        }
        context.notify(Notifications.ID_BACKUP_PROGRESS, backupProgressNotification(progress))
    }

    fun showBackupError(error: String?) {
        context.cancelNotification(Notifications.ID_BACKUP_PROGRESS)

        with(completeNotificationBuilder) {
            setContentTitle(context.stringResource(MR.strings.creating_backup_error))
            setContentText(error)

            show(Notifications.ID_BACKUP_COMPLETE)
        }
    }

    fun showBackupComplete(file: UniFile) {
        context.cancelNotification(Notifications.ID_BACKUP_PROGRESS)

        with(completeNotificationBuilder) {
            setContentTitle(context.stringResource(MR.strings.backup_created))
            setContentText(file.displayablePath)

            clearActions()
            addAction(
                R.drawable.ic_share_24dp,
                context.stringResource(MR.strings.action_share),
                NotificationReceiver.shareBackupPendingActivity(context, file.uri),
            )

            show(Notifications.ID_BACKUP_COMPLETE)
        }
    }

    fun showRestoreProgress(
        content: String = "",
        progress: Int = 0,
        maxAmount: Int = 100,
        sync: Boolean = false,
    ): NotificationCompat.Builder {
        val builder = with(progressNotificationBuilder) {
            val contentTitle = if (sync) {
                context.stringResource(MR.strings.syncing_library)
            } else {
                context.stringResource(MR.strings.restoring_backup)
            }
            setContentTitle(contentTitle)

            if (!preferences.hideNotificationContent.get()) {
                setContentText(content)
            }

            setProgress(maxAmount, progress, false)
            setOnlyAlertOnce(true)

            clearActions()
            addAction(
                R.drawable.ic_close_24dp,
                context.stringResource(MR.strings.action_cancel),
                NotificationReceiver.cancelRestorePendingBroadcast(context, Notifications.ID_RESTORE_PROGRESS),
            )
        }

        builder.show(Notifications.ID_RESTORE_PROGRESS)

        return builder
    }

    fun showRestoreError(error: String?) {
        context.cancelNotification(Notifications.ID_RESTORE_PROGRESS)

        with(completeNotificationBuilder) {
            setContentTitle(context.stringResource(MR.strings.restoring_backup_error))
            setContentText(error)

            show(Notifications.ID_RESTORE_COMPLETE)
        }
    }

    fun showRestoreComplete(
        time: Long,
        errorCount: Int,
        path: String?,
        file: String?,
        sync: Boolean,
    ) {
        val contentTitle = if (sync) {
            context.stringResource(MR.strings.library_sync_complete)
        } else {
            context.stringResource(MR.strings.restore_completed)
        }

        context.cancelNotification(Notifications.ID_RESTORE_PROGRESS)

        val timeString = context.stringResource(
            MR.strings.restore_duration,
            time.milliseconds.inWholeMinutes,
            time.milliseconds.inWholeSeconds - (time.milliseconds.inWholeMinutes * 60),
        )

        with(completeNotificationBuilder) {
            setContentTitle(contentTitle)
            setContentText(
                context.pluralStringResource(
                    MR.plurals.restore_completed_message,
                    errorCount,
                    timeString,
                    errorCount,
                ),
            )

            clearActions()
            if (errorCount > 0 && !path.isNullOrEmpty() && !file.isNullOrEmpty()) {
                val destFile = File(path, file)
                val uri = destFile.getUriCompat(context)

                val errorLogIntent = NotificationReceiver.openErrorLogPendingActivity(context, uri)
                setContentIntent(errorLogIntent)
                addAction(
                    R.drawable.ic_folder_24dp,
                    context.stringResource(MR.strings.action_show_errors),
                    errorLogIntent,
                )
            }

            show(Notifications.ID_RESTORE_COMPLETE)
        }
    }
}

private const val ENTRIES_PROGRESS_INTERVAL_MS = 500L
