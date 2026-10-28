package eu.kanade.presentation.more.settings.screen.textrecognition.playground

import androidx.compose.ui.graphics.ImageBitmap
import mihon.text.recognition.api.image.TextRecognitionImage
import tachiyomi.domain.storage.DocumentUri

/** Decodes the image the user picked to try the recognition settings on. */
fun interface TextRecognitionPlaygroundImageDecoder {
    /** The picked image, or null when it can't be read. Reads it on the calling thread. */
    fun decode(uri: DocumentUri): TextRecognitionPlaygroundImage?
}

/** A picked image as the playground draws the recognized regions on it, and as recognition reads it. */
class TextRecognitionPlaygroundImage(
    val preview: ImageBitmap,
    val recognition: TextRecognitionImage,
)
