package mihon.presentation.kit.files

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable

/** Opens the platform's picker for one [DocumentPickerRequest]; get one from [rememberDocumentPicker]. */
@Stable
interface DocumentPicker {
    /**
     * Opens the picker, with [suggestedName] as the new document's name for [DocumentPickerRequest.CreateDocument];
     * other requests ignore it.
     *
     * @return false when no picker could be opened, such as on a device without a document picker app.
     */
    fun launch(suggestedName: String? = null): Boolean
}

/** [onResult] receives null when the user picked nothing. */
@Composable
expect fun rememberDocumentPicker(
    request: DocumentPickerRequest,
    onResult: (DocumentPick?) -> Unit,
): DocumentPicker
