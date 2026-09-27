package mihon.text.recognition.api.result

import mihon.language.api.tag.LanguageTag
import mihon.text.recognition.api.image.ImageContentKey
import mihon.text.recognition.api.image.ImageRect
import mihon.text.recognition.api.image.ImageSize
import mihon.text.recognition.api.preparation.TextRecognitionPreparation

sealed interface TextRecognitionExecution {
    data class Success(
        val result: TextRecognitionResult,
    ) : TextRecognitionExecution

    /** A prerequisite changed after preparation, for example a model was deleted. */
    data class PreparationChanged(
        val preparation: TextRecognitionPreparation,
    ) : TextRecognitionExecution

    data class Failed(
        val message: String? = null,
    ) : TextRecognitionExecution
}

/** Recognized text of one image, with [regions] in reading order. */
data class TextRecognitionResult(
    val image: ImageContentKey,
    val imageSize: ImageSize,
    val language: LanguageTag,
    val regions: List<RecognizedTextRegion>,
)

/**
 * @property bounds the recognized text in source-image pixels.
 * @property container the enclosing speech bubble when the pipeline detects bubbles; it is where a replacement text
 * can be placed.
 */
data class RecognizedTextRegion(
    val bounds: ImageRect,
    val text: String,
    val kind: TextRegionKind,
    val orientation: TextOrientation,
    val container: ImageRect? = null,
) {
    init {
        require(text.isNotBlank())
    }
}

enum class TextRegionKind {
    SpeechBubble,
    FreeText,
    Unclassified,
}

enum class TextOrientation {
    Horizontal,
    Vertical,
    Unknown,
}
