package mihon.text.recognition.provider.mlkit.catalog

import mihon.text.recognition.api.component.KnownTextRecognitionComponent
import mihon.text.recognition.api.component.TextRecognitionComponentId
import mihon.text.recognition.api.component.TextRecognitionComponentRole
import mihon.text.recognition.api.pipeline.TextRecognitionPipeline
import mihon.text.recognition.api.pipeline.TextRecognitionPreset
import mihon.text.recognition.api.pipeline.TextRecognitionPresetId
import mihon.text.recognition.api.provider.KnownTextRecognitionProvider
import mihon.text.recognition.api.provider.TextRecognitionBuildAvailability
import mihon.text.recognition.api.provider.TextRecognitionProviderId

/**
 * ML Kit's catalog entries. Builds without Google Play services list the provider as not included, so they keep
 * this catalog free of Google libraries.
 */
object MlKitTextRecognitionCatalog {
    fun provider(buildAvailability: TextRecognitionBuildAvailability) = KnownTextRecognitionProvider(
        id = TextRecognitionProviderId("mlkit"),
        name = "ML Kit",
        description = "Google's text recognition. Google Play services downloads the model of each script once, " +
            "after you approve it.",
        processingLocation = "On this device; pages never leave it.",
        buildAvailability = buildAvailability,
        documentationUrl = "https://developers.google.com/ml-kit/vision/text-recognition/v2",
    )

    val recognizer = KnownTextRecognitionComponent(
        id = TextRecognitionComponentId("mlkit.recognizer"),
        provider = TextRecognitionProviderId("mlkit"),
        role = TextRecognitionComponentRole.Recognizer,
        displayName = "ML Kit",
        description = "Reads each detected region line by line. Vertical text is read less reliably.",
        languages = MlKitTextRecognitionScript.entries.flatMapTo(linkedSetOf(), MlKitTextRecognitionScript::languages),
        documentationUrl = "https://developers.google.com/ml-kit/vision/text-recognition/v2/languages",
    )

    /** Uses the bubble detector of the on-device engine to find the regions ML Kit reads. */
    private val comicTextDetector = TextRecognitionComponentId("onnx.comic-text-and-bubble-detector")

    val presets = listOf(
        TextRecognitionPreset(
            id = TextRecognitionPresetId("mlkit.comics"),
            provider = recognizer.provider,
            displayName = "Comics with ML Kit",
            description = "Detects speech bubbles on this device and reads them with ML Kit.",
            languages = recognizer.languages,
            pipeline = TextRecognitionPipeline(comicTextDetector, recognizer.id),
        ),
    )

    const val PROVIDER_ORDER = 150
}
