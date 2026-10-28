package mihon.desktop.backup

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.data.backup.restore.BackupRestorer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import logcat.LogPriority
import mihon.core.metro.AppCoroutineScope
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.backup.model.RestoreOptions
import tachiyomi.domain.backup.service.BackupRestoreRunner
import tachiyomi.domain.storage.DocumentUri

/**
 * Restores backups while the app runs, one at a time, in the application's scope so leaving the screen that started a
 * restore doesn't stop it. Desktop has no app-wide place to show messages yet, so how it ended is logged.
 */
@Inject
@SingleIn(AppScope::class)
class DesktopBackupRestoreRunner(
    @AppCoroutineScope scope: CoroutineScope,
    private val restorerFactory: BackupRestorer.Factory,
    private val reporter: LoggingBackupRestoreReporter,
) : BackupRestoreRunner {
    private val restore = SingleBackgroundRun(scope)

    override suspend fun isRunning(): Boolean = restore.isActive

    override fun start(uri: DocumentUri, options: RestoreOptions, sync: Boolean) {
        restore.start {
            try {
                restorerFactory.create(reporter, sync).restore(uri, options)
            } catch (e: CancellationException) {
                logcat(LogPriority.INFO) { "Restoring the backup was canceled" }
                throw e
            } catch (e: Exception) {
                logcat(LogPriority.ERROR, e) { "Restoring the backup failed" }
            }
        }
    }

    override fun stop() = restore.cancel()
}
