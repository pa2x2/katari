package eu.kanade.presentation.more.settings.screen.data

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.tachiyomi.data.backup.restore.BackupRestoreJob
import eu.kanade.tachiyomi.util.system.DeviceUtil
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.launch
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.i18n.*

/**
 * Starts a backup restore: asks for a backup file and opens [RestoreBackupScreen] for it, unless a restore is
 * already running.
 */
@Composable
fun rememberRestoreBackupLauncher(): () -> Unit {
    val context = LocalContext.current
    val navigator = LocalNavigator.currentOrThrow
    val scope = rememberCoroutineScope()

    val chooseBackup = rememberBackupFilePicker(mimeType = "*/*") {
        if (it == null) {
            context.toast(MR.strings.file_null_uri_error)
            return@rememberBackupFilePicker
        }

        navigator.push(RestoreBackupScreen(it.toString()))
    }

    return {
        scope.launch {
            if (!BackupRestoreJob.isRunning(context)) {
                if (DeviceUtil.isMiui && DeviceUtil.isMiuiOptimizationDisabled()) {
                    context.toast(MR.strings.restore_miui_warning)
                }

                chooseBackup()
            } else {
                context.toast(MR.strings.restore_in_progress)
            }
        }
    }
}

/**
 * Asks for a backup file through the system document picker. [onResult] receives null when nothing was picked.
 */
@Composable
fun rememberBackupFilePicker(mimeType: String, onResult: (Uri?) -> Unit): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            // BackupRestoreJob reads the file later, possibly after the picker's temporary grant is gone. Providers
            // that can't persist grants throw, and the temporary grant still serves an immediate restore.
            try {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (e: SecurityException) {
                context.logcat(LogPriority.WARN, e)
            }
        }
        onResult(uri)
    }

    return {
        try {
            launcher.launch(arrayOf(mimeType))
        } catch (_: ActivityNotFoundException) {
            context.toast(MR.strings.file_picker_error)
        }
    }
}
