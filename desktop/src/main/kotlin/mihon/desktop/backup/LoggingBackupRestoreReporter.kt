package mihon.desktop.backup

import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.data.backup.restore.BackupRestoreReporter
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import java.io.File

/** Logs how a restore ended, since desktop has no app-wide place to show it yet; its progress isn't shown. */
@Inject
class LoggingBackupRestoreReporter : BackupRestoreReporter {
    override fun onRestoreProgress(content: String, progress: Int, maxAmount: Int, sync: Boolean) = Unit

    override fun onRestoreComplete(
        time: Long,
        errorCount: Int,
        path: String?,
        file: String?,
        sync: Boolean,
        restoredProfileNames: List<String>,
    ) {
        logcat(LogPriority.INFO) {
            buildString {
                append("Restored the backup in $time ms with $errorCount errors")
                if (errorCount > 0 && !path.isNullOrEmpty() && !file.isNullOrEmpty()) {
                    append(", listed in ${File(path, file)}")
                }
                if (restoredProfileNames.isNotEmpty()) append("; profiles: ${restoredProfileNames.joinToString()}")
            }
        }
    }
}
