package app.katari.desktop.filechooser.portal

import app.katari.desktop.dbus.Variant
import app.katari.presentation.kit.files.DocumentPickerRequest

/**
 * Linux: the call of the file chooser portal (`org.freedesktop.portal.FileChooser`) that asks for what a
 * [DocumentPickerRequest] asks for: `OpenFile` for a document or an image, with `directory` for a folder, which the
 * interface has from version 3, and `SaveFile` with the suggested name for a new document. [minimumVersion] is the
 * interface version the call needs.
 */
internal data class FileChooserCall(val method: String, val options: Map<String, Variant>, val minimumVersion: Int)

internal const val FILE_CHOOSER = "org.freedesktop.portal.FileChooser"

internal const val FILE_CHOOSER_FOLDER_VERSION = 3

internal fun fileChooserCall(request: DocumentPickerRequest, suggestedName: String?): FileChooserCall = when (request) {
    is DocumentPickerRequest.OpenDocument -> openFile(request.mimeTypes)
    is DocumentPickerRequest.GetContent -> openFile(listOf(request.mimeType))
    DocumentPickerRequest.PickImage -> openFile(listOf("image/*"))
    is DocumentPickerRequest.OpenFolder ->
        FileChooserCall("OpenFile", mapOf("directory" to Variant("b", true)), FILE_CHOOSER_FOLDER_VERSION)
    is DocumentPickerRequest.CreateDocument -> {
        val options = buildMap {
            putAll(mimeTypeFilter(listOf(request.mimeType)))
            suggestedName?.let { put("current_name", Variant("s", it)) }
        }
        FileChooserCall("SaveFile", options, minimumVersion = 1)
    }
}

private fun openFile(mimeTypes: List<String>) = FileChooserCall(
    "OpenFile",
    mimeTypeFilter(mimeTypes),
    minimumVersion = 1,
)

/**
 * One filter of [mimeTypes] that the dialog starts on, so it lists what Android's pickers would; the portal takes MIME
 * types as filters, wildcards for a whole kind included, as Android does. None where any type goes. A filter's name is
 * what the dialog shows for it: the types themselves, since the request carries no description of them.
 */
private fun mimeTypeFilter(mimeTypes: List<String>): Map<String, Variant> {
    if (mimeTypes.isEmpty() || ANY_TYPE in mimeTypes) return emptyMap()
    val filter = listOf(mimeTypes.joinToString(", "), mimeTypes.map { listOf(MIME_TYPE_PATTERN, it) })
    return mapOf(
        "filters" to Variant("a(sa(us))", listOf(filter)),
        "current_filter" to Variant("(sa(us))", filter),
    )
}

private const val ANY_TYPE = "*/*"

// The pattern kind of a filter entry: 0 is a glob such as *.png, 1 a MIME type
private const val MIME_TYPE_PATTERN = 1
