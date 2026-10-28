package mihon.desktop.backup

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.data.backup.restore.BackupRestorer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import logcat.LogPriority
import mihon.core.metro.AppCoroutineScope
import mihon.presentation.kit.feedback.AppMessages
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.backup.model.RestoreOptions
import tachiyomi.domain.backup.service.BackupRestoreRunner
import tachiyomi.domain.storage.DocumentUri
import tachiyomi.i18n.MR
import tachiyomi.i18n.localized.stringResource
import tachiyomi.i18n.restoring_backup_canceled
import tachiyomi.i18n.restoring_backup_error

/**
 * Restores backups while the app runs, one at a time, in the application's scope so leaving the screen that started a
 * restore doesn't stop it. How it ended is told in [messages], through [reporter] when it completes.
 */
@Inject
@SingleIn(AppScope::class)
class DesktopBackupRestoreRunner(
    @AppCoroutineScope scope: CoroutineScope,
    private val restorerFactory: BackupRestorer.Factory,
    private val reporter: AppMessageBackupRestoreReporter,
    private val messages: AppMessages,
) : BackupRestoreRunner {
    private val restore = SingleBackgroundRun(scope)

    override suspend fun isRunning(): Boolean = restore.isActive

    override fun start(uri: DocumentUri, options: RestoreOptions, sync: Boolean) {
        restore.start {
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
    }

    override fun stop() = restore.cancel()
}
