package mihon.desktop.filechooser.toolkit

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mihon.presentation.kit.files.DocumentPickerRequest
import mihon.presentation.kit.files.SystemFileChooser
import tachiyomi.domain.storage.DocumentUri
import java.awt.GraphicsEnvironment
import java.awt.Window
import java.io.File
import javax.swing.JFileChooser

/**
 * The Java runtime's own dialog, Swing's JFileChooser, for desktops without a file chooser of their own to reach. Not
 * AWT's FileDialog: on X11 it shows a GTK dialog that the runtime creates without a parent, so the window manager
 * neither keeps it over the app's window nor treats it as the window's dialog. Swing's dialog is a window of the
 * runtime's toolkit owned by [choose]'s parent, so the toolkit makes it modal to that window and, on X11, transient
 * for it. A dialog can't be taken away once shown, so cancelling a choice ends it only when the user closes the dialog.
 */
internal class ToolkitFileChooser : SystemFileChooser {
    // On the event thread, where a modal dialog's showDialog blocks until the dialog closes; its nested event loop
    // keeps the window recomposing meanwhile
    override suspend fun choose(parent: Window?, request: DocumentPickerRequest, suggestedName: String?): DocumentUri? =
        withContext(Dispatchers.Main) {
            if (GraphicsEnvironment.isHeadless()) return@withContext null
            val chooser = documentChooser(request, suggestedName)
            val answer = when (request) {
                is DocumentPickerRequest.CreateDocument -> chooser.showSaveDialog(parent)
                else -> chooser.showOpenDialog(parent)
            }
            chooser.selectedFile
                ?.takeIf { answer == JFileChooser.APPROVE_OPTION }
                ?.let { DocumentUri(it.toPath().toUri().toString()) }
        }
}

private fun documentChooser(request: DocumentPickerRequest, suggestedName: String?): JFileChooser =
    ReplaceConfirmingFileChooser().apply {
        when (request) {
            is DocumentPickerRequest.OpenDocument -> showOnly(request.mimeTypes)
            is DocumentPickerRequest.GetContent -> showOnly(listOf(request.mimeType))
            DocumentPickerRequest.PickImage -> showOnly(listOf("image/*"))
            is DocumentPickerRequest.CreateDocument -> suggestedName?.let { selectedFile = File(it) }
            is DocumentPickerRequest.OpenFolder -> fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
        }
    }

private fun JFileChooser.showOnly(mimeTypes: List<String>) {
    mimeTypeFileFilter(mimeTypes)?.let { fileFilter = it }
}
