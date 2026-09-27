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

    /**
     * Models this component needs to read [language]; every one must be installed before it runs. Language-independent
     * components return the same models for every language.
     */
    fun models(language: LanguageTag): List<ModelArtifactDescriptor>

    /** Whether this device can run the component for [language] now. */
    suspend fun inspectDevice(language: LanguageTag): TextRecognitionComponentAvailability

    /**
     * Installs models the platform manages for [language], after the user approved the download reported by
     * [TextRecognitionComponentAvailability.PlatformModelsRequired]. Components without such models never report it.
     */
    suspend fun installPlatformModels(language: LanguageTag): TextRecognitionPlatformModelsInstallation =
        TextRecognitionPlatformModelsInstallation.Failed("This component has no platform-managed models")
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

    /**
     * The platform (for example Google Play services) must download models before the component can read the
     * language; it downloads them only after the user approved it.
     */
    data class PlatformModelsRequired(
        val description: String,
        val approximateSizeBytes: Long? = null,
    ) : TextRecognitionComponentAvailability {
        init {
            require(description.isNotBlank())
        }
    }
}

sealed interface TextRecognitionPlatformModelsInstallation {
    data object Installed : TextRecognitionPlatformModelsInstallation

    data class Failed(
        val reason: String,
    ) : TextRecognitionPlatformModelsInstallation {
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
