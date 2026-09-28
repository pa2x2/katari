package mihon.text.recognition.provider.tesseract

import mihon.text.recognition.api.component.KnownTextRecognitionComponent
import mihon.text.recognition.api.component.TextRecognitionComponentId
import mihon.text.recognition.api.component.TextRecognitionComponentRole
import mihon.text.recognition.api.pipeline.TextRecognitionPipeline
import mihon.text.recognition.api.pipeline.TextRecognitionPreset
import mihon.text.recognition.api.pipeline.TextRecognitionPresetId
import mihon.text.recognition.api.provider.KnownTextRecognitionProvider
import mihon.text.recognition.api.provider.TextRecognitionBuildAvailability
import mihon.text.recognition.api.provider.TextRecognitionProviderId

internal object TesseractTextRecognitionCatalog {
    val provider = KnownTextRecognitionProvider(
        id = TextRecognitionProviderId("tesseract"),
        name = "Tesseract",
        description = "The classic open-source OCR engine. Best with clean, printed lettering; each language " +
            "downloads its data once, after you approve it.",
        processingLocation = "On this device; pages never leave it.",
        buildAvailability = TextRecognitionBuildAvailability.Included,
        documentationUrl = "https://github.com/tesseract-ocr/tesseract",
    )

    val recognizer = KnownTextRecognitionComponent(
        id = TextRecognitionComponentId("tesseract.recognizer"),
        provider = provider.id,
        role = TextRecognitionComponentRole.Recognizer,
        displayName = "Tesseract",
        description = "Reads each detected region as a block of text, including vertical Japanese.",
        languages = TesseractLanguage.entries.mapTo(linkedSetOf(), TesseractLanguage::tag),
        documentationUrl = "https://github.com/tesseract-ocr/tessdata_fast",
    )

    /** Uses the bubble detector of the on-device engine to find the regions Tesseract reads. */
    private val comicTextDetector = TextRecognitionComponentId("onnx.comic-text-and-bubble-detector")

    val presets = listOf(
        TextRecognitionPreset(
            id = TextRecognitionPresetId("tesseract.comics"),
            provider = provider.id,
            displayName = "Comics with Tesseract",
            description = "Detects speech bubbles on this device and reads them with Tesseract.",
            languages = recognizer.languages,
            pipeline = TextRecognitionPipeline(comicTextDetector, recognizer.id),
        ),
    )
}
