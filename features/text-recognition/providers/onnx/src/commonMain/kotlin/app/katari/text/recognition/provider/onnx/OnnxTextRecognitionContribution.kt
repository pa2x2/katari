package app.katari.text.recognition.provider.onnx

import app.katari.text.recognition.provider.onnx.catalog.OnnxTextRecognitionCatalog
import app.katari.text.recognition.provider.onnx.detector.ComicTextDetector
import app.katari.text.recognition.provider.onnx.paddle.PaddleOcrRecognizer
import app.katari.text.recognition.provider.onnx.recognizer.MangaOcrRecognizer
import app.katari.text.recognition.provider.onnx.session.OnnxSessions
import app.katari.text.recognition.spi.contribution.TextRecognitionProviderContribution

/** The provider with its components, which share [sessions]; each platform decides when the sessions are freed. */
internal fun onnxTextRecognitionContribution(sessions: OnnxSessions) = TextRecognitionProviderContribution(
    provider = OnnxTextRecognitionCatalog.provider,
    components = OnnxTextRecognitionCatalog.components,
    implementations = listOf(
        ComicTextDetector(sessions),
        MangaOcrRecognizer(sessions),
        PaddleOcrRecognizer(sessions),
    ),
    presets = OnnxTextRecognitionCatalog.presets,
    order = ONNX_PROVIDER_ORDER,
)

private const val ONNX_PROVIDER_ORDER = 100
