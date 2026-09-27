package mihon.text.recognition.provider.onnx.catalog

import mihon.language.api.tag.LanguageTag
import mihon.text.recognition.api.component.KnownTextRecognitionComponent
import mihon.text.recognition.api.component.TextRecognitionComponentId
import mihon.text.recognition.api.component.TextRecognitionComponentRole
import mihon.text.recognition.api.pipeline.TextRecognitionPipeline
import mihon.text.recognition.api.pipeline.TextRecognitionPreset
import mihon.text.recognition.api.pipeline.TextRecognitionPresetId
import mihon.text.recognition.api.provider.KnownTextRecognitionProvider
import mihon.text.recognition.api.provider.TextRecognitionBuildAvailability
import mihon.text.recognition.api.provider.TextRecognitionProviderId
import mihon.text.recognition.provider.onnx.paddle.PaddleOcrScript

internal object OnnxTextRecognitionCatalog {
    val provider = KnownTextRecognitionProvider(
        id = TextRecognitionProviderId("onnx"),
        name = "On-device models",
        description = "Open models that detect speech bubbles and read their text. Each language downloads its " +
            "models once, after you approve them.",
        processingLocation = "On this device; pages never leave it.",
        buildAvailability = TextRecognitionBuildAvailability.Included,
    )

    val comicTextDetector = KnownTextRecognitionComponent(
        id = TextRecognitionComponentId("onnx.comic-text-and-bubble-detector"),
        provider = provider.id,
        role = TextRecognitionComponentRole.Detector,
        displayName = "Comic text and bubble detector",
        description = "Finds speech bubbles and text in manga, webtoons, manhua, and western comics.",
        languages = emptySet(),
        documentationUrl = "https://huggingface.co/ogkalu/comic-text-and-bubble-detector",
    )

    val mangaOcr = KnownTextRecognitionComponent(
        id = TextRecognitionComponentId("onnx.manga-ocr"),
        provider = provider.id,
        role = TextRecognitionComponentRole.Recognizer,
        displayName = "Manga OCR",
        description = "Reads Japanese manga text, including vertical text and furigana.",
        languages = setOf(LanguageTag.require("ja")),
        documentationUrl = "https://github.com/kha-white/manga-ocr",
    )

    val paddleOcr = KnownTextRecognitionComponent(
        id = TextRecognitionComponentId("onnx.paddleocr"),
        provider = provider.id,
        role = TextRecognitionComponentRole.Recognizer,
        displayName = "PaddleOCR",
        description = "Reads printed text line by line in many scripts.",
        languages = PaddleOcrScript.entries.flatMapTo(linkedSetOf(), PaddleOcrScript::languages),
        documentationUrl = "https://github.com/PaddlePaddle/PaddleOCR",
    )

    val components = listOf(comicTextDetector, mangaOcr, paddleOcr)

    val presets: List<TextRecognitionPreset> = listOf(
        TextRecognitionPreset(
            id = TextRecognitionPresetId("onnx.japanese-manga"),
            provider = provider.id,
            displayName = "Japanese manga",
            description = "Detects speech bubbles and reads them with Manga OCR.",
            languages = setOf(LanguageTag.require("ja")),
            pipeline = TextRecognitionPipeline.Staged(comicTextDetector.id, mangaOcr.id),
        ),
    ) + PaddleOcrScript.entries.map { script ->
        TextRecognitionPreset(
            id = TextRecognitionPresetId("onnx.comics-${script.folder}"),
            provider = provider.id,
            displayName = "${script.displayName} comics",
            description = "Detects speech bubbles and reads them with PaddleOCR.",
            languages = script.languages,
            pipeline = TextRecognitionPipeline.Staged(comicTextDetector.id, paddleOcr.id),
        )
    }
}
