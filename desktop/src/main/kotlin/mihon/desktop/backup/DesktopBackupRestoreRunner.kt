package mihon.desktop.backup

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.data.backup.restore.BackupRestorer
import kotlinx.coroutines.CancellationException
import logcat.LogPriority
import mihon.desktop.work.ExistingWorkPolicy
import mihon.desktop.work.InProcessWork
import mihon.desktop.work.InProcessWorkRequest
import mihon.desktop.work.ResumableWork
import mihon.presentation.kit.feedback.AppMessages
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.backup.model.RestoreOptions
import tachiyomi.domain.backup.service.BackupRestoreRunner
import tachiyomi.domain.storage.DocumentUri
import tachiyomi.domain.work.WorkOutcome
import tachiyomi.i18n.MR
import tachiyomi.i18n.localized.stringResource
import tachiyomi.i18n.restoring_backup_canceled
import tachiyomi.i18n.restoring_backup_error

/**
 * Restores backups as [InProcessWork], one at a time, so leaving the screen that started a restore doesn't stop it.
 * How it ended is told in [messages], through [reporter] when it completes.
 */
@Inject
@SingleIn(AppScope::class)
class DesktopBackupRestoreRunner(
    private val work: InProcessWork,
    private val restorerFactory: BackupRestorer.Factory,
    private val reporter: AppMessageBackupRestoreReporter,
    private val messages: AppMessages,
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
            restorerFactory.create(reporter, sync).restore(uri, options)
        } catch (e: CancellationException) {
            logcat(LogPriority.INFO) { "Restoring the backup was canceled" }
            messages.show(stringResource(MR.strings.restoring_backup_canceled))
            throw e
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e) { "Restoring the backup failed" }
            messages.show(stringResource(MR.strings.restoring_backup_error))
        }
    }

    private companion object {
        // The name of Android's restore work
        const val NAME = "BackupRestore"
    }
}
