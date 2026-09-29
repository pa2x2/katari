package eu.kanade.presentation.more.settings.screen.data

import android.content.Context
import android.content.Intent
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
import tachiyomi.core.common.i18n.stringResource
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

    val chooseBackup = rememberLauncherForActivityResult(
        object : ActivityResultContracts.GetContent() {
            override fun createIntent(context: Context, input: String): Intent {
                val intent = super.createIntent(context, input)
                return Intent.createChooser(intent, context.stringResource(MR.strings.file_select_backup))
            }
        },
    ) {
        if (it == null) {
            context.toast(MR.strings.file_null_uri_error)
            return@rememberLauncherForActivityResult
        }

        navigator.push(RestoreBackupScreen(it.toString()))
    }

    return {
        scope.launch {
            if (!BackupRestoreJob.isRunning(context)) {
                if (DeviceUtil.isMiui && DeviceUtil.isMiuiOptimizationDisabled()) {
                    context.toast(MR.strings.restore_miui_warning)
                }

                // no need to catch because it's wrapped with a chooser
                chooseBackup.launch("*/*")
            } else {
                context.toast(MR.strings.restore_in_progress)
            }
        }
    }
}
