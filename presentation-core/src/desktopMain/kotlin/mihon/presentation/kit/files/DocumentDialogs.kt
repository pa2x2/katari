package mihon.presentation.kit.files

import java.awt.Dialog
import java.awt.FileDialog
import java.awt.Frame
import java.awt.Window
import java.io.File
import javax.swing.JFileChooser

// Interim until the XDG desktop portal's file chooser: AWT's FileDialog for files, and Swing's JFileChooser for folders
// because FileDialog can't choose a folder on Linux.
internal fun showDocumentDialog(parent: Window?, request: DocumentPickerRequest, suggestedName: String?): File? =
    when (request) {
        is DocumentPickerRequest.OpenDocument -> openFile(parent, request.mimeTypes)
        is DocumentPickerRequest.GetContent -> openFile(parent, listOf(request.mimeType))
        DocumentPickerRequest.PickImage -> openFile(parent, listOf("image/*"))
        is DocumentPickerRequest.CreateDocument -> saveFile(parent, suggestedName)
        is DocumentPickerRequest.OpenFolder -> openFolder(parent)
    }

private fun openFile(parent: Window?, mimeTypes: List<String>): File? {
    val dialog = fileDialog(parent, FileDialog.LOAD)
    mimeTypeFilenameFilter(mimeTypes)?.let(dialog::setFilenameFilter)
    return dialog.showForFile()
}

private fun saveFile(parent: Window?, suggestedName: String?): File? {
    val dialog = fileDialog(parent, FileDialog.SAVE)
    dialog.file = suggestedName
    return dialog.showForFile()
}

private fun openFolder(parent: Window?): File? {
    val chooser = JFileChooser().apply { fileSelectionMode = JFileChooser.DIRECTORIES_ONLY }
    return if (chooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) chooser.selectedFile else null
}

private fun fileDialog(parent: Window?, mode: Int): FileDialog = when (parent) {
    is Frame -> FileDialog(parent, "", mode)
    is Dialog -> FileDialog(parent, "", mode)
    else -> FileDialog(null as Frame?, "", mode)
}

private fun FileDialog.showForFile(): File? {
    isVisible = true
    val picked = file?.let { File(directory, it) }
    dispose()
    return picked
}
