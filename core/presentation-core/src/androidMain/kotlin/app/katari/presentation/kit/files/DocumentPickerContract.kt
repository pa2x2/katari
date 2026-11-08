package app.katari.presentation.kit.files

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts

/** The system picker contract for a request, taking the suggested name [DocumentPicker.launch] gets as its input. */
internal fun DocumentPickerRequest.contract(): ActivityResultContract<String?, Uri?> = when (this) {
    is DocumentPickerRequest.OpenDocument ->
        ActivityResultContracts.OpenDocument().withInput { mimeTypes.toTypedArray() }
    is DocumentPickerRequest.OpenFolder ->
        ActivityResultContracts.OpenDocumentTree().withInput { null }
    is DocumentPickerRequest.CreateDocument ->
        ActivityResultContracts.CreateDocument(mimeType).withInput { it.orEmpty() }
    is DocumentPickerRequest.GetContent ->
        ActivityResultContracts.GetContent().withInput { mimeType }
    DocumentPickerRequest.PickImage ->
        ActivityResultContracts.PickVisualMedia().withInput {
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        }
}

private fun <I> ActivityResultContract<I, Uri?>.withInput(
    inputFor: (suggestedName: String?) -> I,
): ActivityResultContract<String?, Uri?> {
    val contract = this
    return object : ActivityResultContract<String?, Uri?>() {
        override fun createIntent(context: Context, input: String?): Intent =
            contract.createIntent(context, inputFor(input))

        override fun getSynchronousResult(context: Context, input: String?): SynchronousResult<Uri?>? =
            contract.getSynchronousResult(context, inputFor(input))

        override fun parseResult(resultCode: Int, intent: Intent?): Uri? = contract.parseResult(resultCode, intent)
    }
}
