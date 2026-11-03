package eu.kanade.presentation.more.settings.screen.data

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kotlinx.coroutines.launch
import mihon.app.ui.di.LocalAppUiGraph
import mihon.presentation.kit.feedback.rememberScreenMessages
import mihon.presentation.kit.files.DocumentAccess
import mihon.presentation.kit.files.DocumentPickerRequest
import mihon.presentation.kit.files.rememberDocumentPicker
import tachiyomi.domain.storage.DocumentUri
import tachiyomi.i18n.*

/**
 * Starts a backup restore: asks for a backup file and opens [RestoreBackupScreen] for it, unless a restore is
 * already running.
 */
@Composable
fun rememberRestoreBackupLauncher(): () -> Unit {
    val graph = LocalAppUiGraph.current
    val backupRestoreRunner = remember(graph) { graph.backupRestoreRunner }
    val backupSystemWarning = remember(graph) { graph.backupSystemWarning }
    val messages = rememberScreenMessages()
    val navigator = LocalNavigator.currentOrThrow
    val scope = rememberCoroutineScope()

    val chooseBackup = rememberBackupFilePicker(mimeType = "*/*") {
        if (it == null) {
            messages.show(MR.strings.file_null_uri_error)
            return@rememberBackupFilePicker
        }

        navigator.push(RestoreBackupScreen(it))
    }

    return {
        scope.launch {
            if (!backupRestoreRunner.isRunning()) {
                if (backupSystemWarning.applies) {
                    messages.show(MR.strings.restore_miui_warning)
                }

                chooseBackup()
            } else {
                messages.show(MR.strings.restore_in_progress)
            }
        }
    }
}

/**
 * Asks for a backup file through the system document picker. [onResult] receives null when nothing was picked.
 */
@Composable
fun rememberBackupFilePicker(mimeType: String, onResult: (DocumentUri?) -> Unit): () -> Unit {
    val messages = rememberScreenMessages()
    // The restore reads the file later, possibly after the picker's temporary grant is gone. Android providers that
    // can't persist grants throw, and the temporary grant still serves an immediate restore.
    val picker = rememberDocumentPicker(
        DocumentPickerRequest.OpenDocument(listOf(mimeType), keepAccess = DocumentAccess.Read),
    ) { pick ->
        onResult(pick?.uri)
    }

    return {
        if (!picker.launch()) {
            messages.show(MR.strings.file_picker_error)
        }
    }
}
