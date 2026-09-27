package mihon.text.recognition.provider.onnx.catalog

import mihon.language.api.tag.LanguageTag
import mihon.text.recognition.api.component.KnownTextRecognitionComponent
import mihon.text.recognition.api.component.TextRecognitionBuildAvailability
import mihon.text.recognition.api.component.TextRecognitionComponentId
import mihon.text.recognition.api.component.TextRecognitionComponentRole
import mihon.text.recognition.api.pipeline.TextRecognitionPipeline
import mihon.text.recognition.api.pipeline.TextRecognitionPreset
import mihon.text.recognition.api.pipeline.TextRecognitionPresetId

internal object OnnxTextRecognitionCatalog {
    private const val PROVIDER_NAME = "On-device models"

    val comicTextDetector = KnownTextRecognitionComponent(
        id = TextRecognitionComponentId("onnx.comic-text-and-bubble-detector"),
        role = TextRecognitionComponentRole.Detector,
        providerName = PROVIDER_NAME,
        displayName = "Comic text and bubble detector",
        description = "Finds speech bubbles and text in manga, webtoons, manhua, and western comics. " +
            "Runs on this device.",
        languages = emptySet(),
        buildAvailability = TextRecognitionBuildAvailability.Included,
        documentationUrl = "https://huggingface.co/ogkalu/comic-text-and-bubble-detector",
    )

    val mangaOcr = KnownTextRecognitionComponent(
        id = TextRecognitionComponentId("onnx.manga-ocr"),
        role = TextRecognitionComponentRole.Recognizer,
        providerName = PROVIDER_NAME,
        displayName = "Manga OCR",
        description = "Reads Japanese manga text, including vertical text and furigana. Runs on this device.",
        languages = setOf(LanguageTag.require("ja")),
        buildAvailability = TextRecognitionBuildAvailability.Included,
        documentationUrl = "https://github.com/kha-white/manga-ocr",
    )

    val japaneseMangaPreset = TextRecognitionPreset(
        id = TextRecognitionPresetId("onnx.japanese-manga"),
        displayName = "Japanese manga",
        description = "Detects speech bubbles and reads them with Manga OCR, entirely on this device.",
        languages = setOf(LanguageTag.require("ja")),
        pipeline = TextRecognitionPipeline.Staged(
            detector = comicTextDetector.id,
            recognizer = mangaOcr.id,
        ),
    )
}
