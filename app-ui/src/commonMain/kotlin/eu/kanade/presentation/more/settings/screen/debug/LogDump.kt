package eu.kanade.presentation.more.settings.screen.debug

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import app.katari.core.platform.currentPlatformContext
import app.katari.presentation.kit.files.DocumentPickerRequest
import app.katari.presentation.kit.files.rememberDocumentPicker
import app.katari.ui.di.LocalAppUiGraph
import kotlinx.coroutines.launch

/**
 * Dumps the app's logs, with [exception] when the crash screen reports one, the way the platform hands them over:
 * into a document the user picks, or to the share sheet.
 */
@Composable
fun rememberLogDump(exception: Throwable? = null): () -> Unit {
    val diagnostics = LocalAppUiGraph.current.deviceDiagnostics
    val context = currentPlatformContext()
    val scope = rememberCoroutineScope()
    // The platform decides this once, so the picker is either always remembered here or never
    if (!diagnostics.savesLogsToPickedDocument) {
        return { scope.launch { diagnostics.dumpLogs(context, exception) } }
    }
    val picker = rememberDocumentPicker(DocumentPickerRequest.CreateDocument(LOG_DUMP_MIME_TYPE)) { pick ->
        if (pick != null) scope.launch { diagnostics.dumpLogsToDocument(pick.uri, exception) }
    }
    return { picker.launch(LOG_DUMP_NAME) }
}

const val LOG_DUMP_NAME = "katari_crash_logs.txt"

private const val LOG_DUMP_MIME_TYPE = "text/plain"
