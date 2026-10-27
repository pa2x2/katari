package mihon.presentation.kit.files

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.awt.LocalAwtWindow
import tachiyomi.domain.storage.DocumentUri
import java.awt.GraphicsEnvironment
import java.awt.Window
import javax.swing.SwingUtilities

@Composable
actual fun rememberDocumentPicker(
    request: DocumentPickerRequest,
    onResult: (DocumentPick?) -> Unit,
): DocumentPicker {
    val window = LocalAwtWindow.current
    val currentRequest = rememberUpdatedState(request)
    val currentOnResult = rememberUpdatedState(onResult)
    return remember(window) { DialogDocumentPicker(window, currentRequest, currentOnResult) }
}

private class DialogDocumentPicker(
    private val window: Window?,
    private val request: State<DocumentPickerRequest>,
    private val onResult: State<(DocumentPick?) -> Unit>,
) : DocumentPicker {
    override fun launch(suggestedName: String?): Boolean {
        if (GraphicsEnvironment.isHeadless()) return false
        // Shown in a later event because a modal dialog's setVisible blocks until the dialog closes; its nested event
        // loop keeps the window recomposing meanwhile. Compose's AwtWindow shows dialogs the same way.
        SwingUtilities.invokeLater {
            val current = request.value
            val file = showDocumentDialog(window, current, suggestedName)
            onResult.value(
                file?.let {
                    // Files need no grant, so desktop keeps whatever access a request asks for
                    DocumentPick(DocumentUri(it.toPath().toUri().toString()), accessKept = current.accessToKeep != null)
                },
            )
        }
        return true
    }
}
