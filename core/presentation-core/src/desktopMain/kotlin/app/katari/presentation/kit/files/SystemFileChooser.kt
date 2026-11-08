package app.katari.presentation.kit.files

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import tachiyomi.domain.storage.DocumentUri
import java.awt.Window

/** The desktop's own dialog for choosing files and folders, which [rememberDocumentPicker] opens on desktop. */
interface SystemFileChooser {
    /**
     * Asks the user for what [request] asks for in a dialog over [parent], with [suggestedName] as the new file's name
     * for [DocumentPickerRequest.CreateDocument], and returns the `file://` URI of the file or folder chosen; null when
     * the user chose nothing or the dialog failed. Cancelling the call takes the dialog away.
     */
    suspend fun choose(parent: Window?, request: DocumentPickerRequest, suggestedName: String?): DocumentUri?
}

/** Provided by the desktop app's window around its content. */
val LocalSystemFileChooser: ProvidableCompositionLocal<SystemFileChooser> = staticCompositionLocalOf {
    error("No SystemFileChooser provided around this window's content")
}
