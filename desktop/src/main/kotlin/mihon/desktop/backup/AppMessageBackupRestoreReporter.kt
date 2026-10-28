package mihon.desktop.backup

import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.data.backup.restore.BackupRestoreReporter
import logcat.LogPriority
import mihon.presentation.kit.feedback.AppMessages
import tachiyomi.core.common.util.system.logcat
import tachiyomi.i18n.MR
import tachiyomi.i18n.library_sync_complete
import tachiyomi.i18n.localized.pluralStringResource
import tachiyomi.i18n.localized.stringResource
import tachiyomi.i18n.restore_completed
import tachiyomi.i18n.restore_completed_message
import tachiyomi.i18n.restore_completed_profiles
import tachiyomi.i18n.restore_duration
import java.io.File
import kotlin.time.Duration.Companion.milliseconds

/**
 * Tells how a restore ended in [messages], as Android's notification does, and logs where its errors are listed;
 * desktop shows no progress.
 */
@Inject
class AppMessageBackupRestoreReporter(private val messages: AppMessages) : BackupRestoreReporter {
    override fun onRestoreProgress(content: String, progress: Int, maxAmount: Int, sync: Boolean) = Unit

    override fun onRestoreComplete(
        time: Long,
        errorCount: Int,
        path: String?,
        file: String?,
        sync: Boolean,
        restoredProfileNames: List<String>,
    ) {
        if (errorCount > 0 && !path.isNullOrEmpty() && !file.isNullOrEmpty()) {
            logcat(LogPriority.INFO) { "Restore errors are listed in ${File(path, file)}" }
        }
        val duration = time.milliseconds
        val timeString = stringResource(
            MR.strings.restore_duration,
            duration.inWholeMinutes,
            duration.inWholeSeconds - duration.inWholeMinutes * 60,
        )
        val lines = buildList {
            add(stringResource(if (sync) MR.strings.library_sync_complete else MR.strings.restore_completed))
            add(pluralStringResource(MR.plurals.restore_completed_message, errorCount, timeString, errorCount))
            if (restoredProfileNames.isNotEmpty()) {
                add(stringResource(MR.strings.restore_completed_profiles, restoredProfileNames.joinToString()))
            }
        }
        messages.show(lines.joinToString("\n"), long = true)
    }
}
