package app.katari.desktop.backup

import app.katari.desktop.work.ExistingWorkPolicy
import app.katari.desktop.work.InProcessWork
import app.katari.desktop.work.InProcessWorkRequest
import app.katari.desktop.work.ResumableWork
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.data.backup.restore.BackupRestorer
import kotlinx.coroutines.CancellationException
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.backup.model.RestoreOptions
import tachiyomi.domain.backup.service.BackupRestoreRunner
import tachiyomi.domain.storage.DocumentUri
import tachiyomi.domain.work.WorkOutcome
import tachiyomi.i18n.MR
import tachiyomi.i18n.localized.stringResource
import tachiyomi.i18n.restoring_backup_canceled

/**
 * Restores backups as [InProcessWork], one at a time, so leaving the screen that started a restore doesn't stop it.
 * Its progress and how it ended are told through [notifier], as Android's restore job tells them.
 */
@Inject
@SingleIn(AppScope::class)
class DesktopBackupRestoreRunner(
    private val work: InProcessWork,
    private val restorerFactory: BackupRestorer.Factory,
    private val notifier: DesktopBackupNotifier,
) : BackupRestoreRunner, ResumableWork {
    override val kinds: Set<String> = setOf(NAME)

    override suspend fun isRunning(): Boolean = work.currentWorks(NAME).any { it.isRunning }

    override fun start(uri: DocumentUri, options: RestoreOptions, sync: Boolean) {
        work.enqueue(
            NAME,
            ExistingWorkPolicy.Keep,
            InProcessWorkRequest(resumeAs = NAME, input = BackupRestoreInput.encode(uri, options, sync)) {
                restore(uri, options, sync)
                WorkOutcome.Done
            },
        )
    }

    override fun stop() = work.cancel(NAME)

    override suspend fun resume(kind: String, input: Map<String, String>) {
        BackupRestoreInput.decode(input)?.let { (uri, options, sync) -> start(uri, options, sync) }
    }

    private suspend fun restore(uri: DocumentUri, options: RestoreOptions, sync: Boolean) {
        try {
            restorerFactory.create(notifier, sync).restore(uri, options)
        } catch (e: CancellationException) {
            logcat(LogPriority.INFO) { "Restoring the backup was canceled" }
            notifier.showRestoreError(stringResource(MR.strings.restoring_backup_canceled))
            throw e
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e) { "Restoring the backup failed" }
            notifier.showRestoreError(e.message)
        } finally {
            notifier.withdrawRestoreProgress()
        }
    }

    private companion object {
        // The name of Android's restore work
        const val NAME = "BackupRestore"
    }
}
