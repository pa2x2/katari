package mihon.text.recognition.spi.component

import android.graphics.Bitmap
import mihon.language.api.tag.LanguageTag
import mihon.model.artifacts.api.descriptor.ModelArtifactDescriptor
import mihon.text.recognition.api.component.KnownTextRecognitionComponent
import mihon.text.recognition.api.image.ImageRect
import mihon.text.recognition.api.result.TextOrientation
import mihon.text.recognition.spi.model.TextRecognitionModels

/**
 * Internal provider adapter for one pipeline stage. Coordinates exchanged with components are pixels of the bitmap
 * the component received; the runtime maps them back to source-image coordinates.
 */
sealed interface TextRecognitionComponent {
    val catalogEntry: KnownTextRecognitionComponent

    /** Models this component needs; every one must be installed before it runs. */
    val models: List<ModelArtifactDescriptor>

    /** Request-independent device inspection. */
    suspend fun inspectDevice(): TextRecognitionComponentAvailability
}

sealed interface TextRecognitionComponentAvailability {
    data object Available : TextRecognitionComponentAvailability

    data class Unavailable(
        val reason: String,
    ) : TextRecognitionComponentAvailability {
        init {
            require(reason.isNotBlank())
        }
    }
}

interface TextDetector : TextRecognitionComponent {
    /**
     * Edge length the detector scales its input to. The runtime tiles tall images so that no tile is much longer than
     * it is wide, which keeps text legible after the detector's own resize.
     */
    val inputEdge: Int

    suspend fun detect(tile: Bitmap, models: TextRecognitionModels): List<DetectedTextRegion>
}

data class DetectedTextRegion(
    val bounds: ImageRect,
    val kind: DetectedTextRegionKind,
    val confidence: Float,
)

enum class DetectedTextRegionKind {
    /** A speech bubble outline; a container for text, not text itself. */
    Bubble,

    /** Text inside a speech bubble. */
    BubbleText,

    /** Text outside speech bubbles, such as narration or sound effects. */
    FreeText,
}

interface TextRecognizer : TextRecognitionComponent {
    /** Edge length the recognizer scales crops to; crops are decoded at least this large when the source allows. */
    val inputEdge: Int

    suspend fun recognize(crop: Bitmap, language: LanguageTag, models: TextRecognitionModels): RecognizedCropText?
}

data class RecognizedCropText(
    val text: String,
    val orientation: TextOrientation = TextOrientation.Unknown,
)

interface TextRecognitionEngine : TextRecognitionComponent {
    /** Largest edge the engine reads reliably; the runtime tiles and subsamples larger images. */
    val maximumInputEdge: Int

    suspend fun recognize(
        image: Bitmap,
        language: LanguageTag,
        models: TextRecognitionModels,
    ): List<EngineTextRegion>
}

data class EngineTextRegion(
    val bounds: ImageRect,
    val text: String,
    val orientation: TextOrientation = TextOrientation.Unknown,
)
