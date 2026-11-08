package app.katari.presentation.kit.files

/**
 * What a [DocumentPicker] asks the user for. Each kind is one of Android's system pickers, so a screen moved to shared
 * code keeps the picker it had on Android.
 *
 * `keepAccess` is the access to keep after the app restarts; null keeps only the picker's temporary grant.
 */
sealed interface DocumentPickerRequest {
    /** An existing document matching any of [mimeTypes]; a type may be a wildcard, as Android's pickers allow. */
    data class OpenDocument(
        val mimeTypes: List<String>,
        val keepAccess: DocumentAccess? = null,
    ) : DocumentPickerRequest

    data class OpenFolder(val keepAccess: DocumentAccess? = null) : DocumentPickerRequest

    /** A new document of [mimeType], named after the suggestion passed to [DocumentPicker.launch]. */
    data class CreateDocument(
        val mimeType: String,
        val keepAccess: DocumentAccess? = null,
    ) : DocumentPickerRequest

    /** Content of [mimeType] from any app that offers it, not only from document providers. */
    data class GetContent(val mimeType: String) : DocumentPickerRequest

    /** An image from the system's photo picker. */
    data object PickImage : DocumentPickerRequest
}

internal val DocumentPickerRequest.accessToKeep: DocumentAccess?
    get() = when (this) {
        is DocumentPickerRequest.OpenDocument -> keepAccess
        is DocumentPickerRequest.OpenFolder -> keepAccess
        is DocumentPickerRequest.CreateDocument -> keepAccess
        is DocumentPickerRequest.GetContent, DocumentPickerRequest.PickImage -> null
    }

enum class DocumentAccess {
    Read,
    ReadWrite,
}
