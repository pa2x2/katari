package mihon.desktop.backup

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.data.backup.create.BackupCreator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import logcat.LogPriority
import mihon.core.metro.AppCoroutineScope
import mihon.presentation.kit.feedback.AppMessages
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.backup.model.BackupOptions
import tachiyomi.domain.backup.service.BackupCreateSchedule
import tachiyomi.domain.storage.DocumentUri
import tachiyomi.i18n.MR
import tachiyomi.i18n.backup_created
import tachiyomi.i18n.creating_backup_error
import tachiyomi.i18n.localized.stringResource

/**
 * Creates the backups the user starts while the app runs, one at a time, in the application's scope, and tells how
 * each ended in [messages].
 *
 * Automatic backups are not offered on desktop yet, so the backup settings hide their interval and [setupTask] does
 * nothing.
 */
@Inject
@SingleIn(AppScope::class)
class DesktopBackupCreateSchedule(
    @AppCoroutineScope scope: CoroutineScope,
    private val creatorFactory: BackupCreator.Factory,
    private val messages: AppMessages,
) : BackupCreateSchedule {
    private val manualBackup = SingleBackgroundRun(scope)

    override val isAutomaticBackupOffered: Boolean = false

    override suspend fun setupTask(prefInterval: Int?) = Unit

    override fun startNow(uri: DocumentUri, options: BackupOptions) {
        manualBackup.start {
            try {
                val location = creatorFactory.create(isAutoBackup = false, onProgress = {}).backup(uri, options)
                logcat(LogPriority.INFO) { "Created the backup ${location.value}" }
                messages.show(stringResource(MR.strings.backup_created))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logcat(LogPriority.ERROR, e) { "Creating the backup failed" }
                messages.show(stringResource(MR.strings.creating_backup_error))
            }
        }
    }

    override suspend fun isManualBackupRunning(): Boolean = manualBackup.isActive
}
