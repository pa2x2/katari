package mihon.desktop.backup

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.data.backup.create.BackupCreator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import logcat.LogPriority
import mihon.core.metro.AppCoroutineScope
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.backup.model.BackupOptions
import tachiyomi.domain.backup.service.BackupCreateSchedule
import tachiyomi.domain.storage.DocumentUri

/**
 * Creates the backups the user starts while the app runs, one at a time, in the application's scope. Desktop has no
 * app-wide place to show messages yet, so how a backup ended is logged.
 *
 * Automatic backups are not offered on desktop yet. The backup settings have no way to hide their interval, so
 * [setupTask] does nothing and the interval setting still shows.
 */
@Inject
@SingleIn(AppScope::class)
class DesktopBackupCreateSchedule(
    @AppCoroutineScope scope: CoroutineScope,
    private val creatorFactory: BackupCreator.Factory,
) : BackupCreateSchedule {
    private val manualBackup = SingleBackgroundRun(scope)

    override suspend fun setupTask(prefInterval: Int?) = Unit

    override fun startNow(uri: DocumentUri, options: BackupOptions) {
        manualBackup.start {
            try {
                val location = creatorFactory.create(isAutoBackup = false, onProgress = {}).backup(uri, options)
                logcat(LogPriority.INFO) { "Created the backup ${location.value}" }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logcat(LogPriority.ERROR, e) { "Creating the backup failed" }
            }
        }
    }

    override suspend fun isManualBackupRunning(): Boolean = manualBackup.isActive
}
