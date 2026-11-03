package mihon.desktop.backup

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.data.backup.create.AutomaticBackupLocation
import eu.kanade.tachiyomi.data.backup.create.BackupCreator
import kotlinx.coroutines.CancellationException
import logcat.LogPriority
import mihon.desktop.work.ExistingWorkPolicy
import mihon.desktop.work.InProcessWork
import mihon.desktop.work.InProcessWorkRequest
import mihon.desktop.work.ResumableWork
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.backup.model.BackupOptions
import tachiyomi.domain.backup.service.BackupCreateSchedule
import tachiyomi.domain.backup.service.BackupPreferences
import tachiyomi.domain.backup.service.BackupRestoreRunner
import tachiyomi.domain.storage.DocumentUri
import tachiyomi.domain.work.WorkBackoff
import tachiyomi.domain.work.WorkOutcome
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

/**
 * Creates backups as [InProcessWork]: the ones the user starts, one at a time, and the automatic ones at the backup
 * interval while the app is open, the one the app missed while closed at the next launch (D019). As on Android, both
 * show their progress through [notifier], and only the user's tell how they ended.
 */
@Inject
@SingleIn(AppScope::class)
class DesktopBackupCreateSchedule(
    private val work: InProcessWork,
    private val creatorFactory: BackupCreator.Factory,
    private val automaticBackupLocation: AutomaticBackupLocation,
    private val backupPreferences: BackupPreferences,
    // Restoring sets up the automatic backups again, which this does.
    private val restoreRunner: Lazy<BackupRestoreRunner>,
    private val notifier: DesktopBackupNotifier,
) : BackupCreateSchedule, ResumableWork {
    override val isAutomaticBackupOffered: Boolean = true

    override val kinds: Set<String> = setOf(NAME_AUTOMATIC, NAME_MANUAL)

    override suspend fun setupTask(prefInterval: Int?) {
        val interval = prefInterval ?: backupPreferences.backupInterval.get()
        if (interval > 0) {
            work.schedulePeriodic(
                NAME_AUTOMATIC,
                interval.hours,
                InProcessWorkRequest(resumeAs = NAME_AUTOMATIC, backoff = AUTOMATIC_BACKOFF) { backUpAutomatically() },
            )
        } else {
            work.cancelPeriodic(NAME_AUTOMATIC)
        }
    }

    override fun startNow(uri: DocumentUri, options: BackupOptions) {
        work.enqueue(
            NAME_MANUAL,
            ExistingWorkPolicy.Keep,
            InProcessWorkRequest(resumeAs = NAME_MANUAL, input = BackupCreateInput.encode(uri, options)) {
                backUp(uri, options)
                WorkOutcome.Done
            },
        )
    }

    override suspend fun isManualBackupRunning(): Boolean = work.currentWorks(NAME_MANUAL).any { it.isRunning }

    override suspend fun resume(kind: String, input: Map<String, String>) {
        when (kind) {
            NAME_AUTOMATIC -> setupTask()
            NAME_MANUAL -> BackupCreateInput.decode(input)?.let { (uri, options) -> startNow(uri, options) }
        }
    }

    private suspend fun backUpAutomatically(): WorkOutcome {
        if (restoreRunner.value.isRunning()) return WorkOutcome.RetryLater
        val folder = automaticBackupLocation.folder() ?: return WorkOutcome.Failed
        return try {
            creatorFactory.create(isAutoBackup = true, onProgress = notifier::showBackupProgress)
                .backup(automaticBackupLocation.next(folder), BackupOptions())
            WorkOutcome.Done
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e) { "Creating the automatic backup failed" }
            WorkOutcome.Failed
        } finally {
            notifier.withdrawBackupProgress()
        }
    }

    private suspend fun backUp(uri: DocumentUri, options: BackupOptions) {
        try {
            val location = creatorFactory.create(isAutoBackup = false, onProgress = notifier::showBackupProgress)
                .backup(uri, options)
            logcat(LogPriority.INFO) { "Created the backup ${location.value}" }
            notifier.showBackupComplete(location)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e) { "Creating the backup failed" }
            notifier.showBackupError(e.message)
        } finally {
            notifier.withdrawBackupProgress()
        }
    }

    private companion object {
        // The names of Android's backup work
        const val NAME_AUTOMATIC = "BackupCreator"
        const val NAME_MANUAL = "$NAME_AUTOMATIC:manual"

        val AUTOMATIC_BACKOFF = WorkBackoff(initialDelay = 10.minutes)
    }
}
