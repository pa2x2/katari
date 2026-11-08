package mihon.presentation.kit.files

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.awt.LocalAwtWindow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.awt.Window

@Composable
actual fun rememberDocumentPicker(
    request: DocumentPickerRequest,
    onResult: (DocumentPick?) -> Unit,
): DocumentPicker {
    val window = LocalAwtWindow.current
    val chooser = LocalSystemFileChooser.current
    // A choice still open when the picker leaves the composition is taken away, and its result goes nowhere
    val scope = rememberCoroutineScope()
    val currentRequest = rememberUpdatedState(request)
    val currentOnResult = rememberUpdatedState(onResult)
    return remember(window, chooser, scope) {
        ChooserDocumentPicker(window, chooser, scope, currentRequest, currentOnResult)
    }
}

private class ChooserDocumentPicker(
    private val window: Window?,
    private val chooser: SystemFileChooser,
    private val scope: CoroutineScope,
    private val request: State<DocumentPickerRequest>,
    private val onResult: State<(DocumentPick?) -> Unit>,
) : DocumentPicker {
    override fun launch(suggestedName: String?): Boolean {
        val current = request.value
        scope.launch {
            val uri = chooser.choose(window, current, suggestedName)
            // Files need no grant, so desktop keeps whatever access a request asks for
            onResult.value(uri?.let { DocumentPick(it, accessKept = current.accessToKeep != null) })
        }
        return true
    }
}
