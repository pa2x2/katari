package mihon.text.recognition.provider.onnx

import android.app.Application
import mihon.feature.runtime.application.ApplicationFeatureRuntimeComponent
import mihon.text.recognition.provider.onnx.catalog.OnnxTextRecognitionCatalog
import mihon.text.recognition.provider.onnx.detector.ComicTextDetector
import mihon.text.recognition.provider.onnx.recognizer.MangaOcrRecognizer
import mihon.text.recognition.provider.onnx.session.OnnxSessions
import mihon.text.recognition.runtime.component.TextRecognitionRuntimeComponent
import mihon.text.recognition.runtime.component.TextRecognitionRuntimeContribution
import mihon.text.recognition.spi.contribution.TextRecognitionComponentContribution
import mihon.text.recognition.spi.contribution.TextRecognitionPresetContribution

val onnxTextRecognitionRuntimeComponent: ApplicationFeatureRuntimeComponent =
    object : TextRecognitionRuntimeComponent {
        override fun contribute(application: Application): TextRecognitionRuntimeContribution {
            val sessions = OnnxSessions()
            return TextRecognitionRuntimeContribution(
                components = listOf(
                    TextRecognitionComponentContribution(ComicTextDetector(sessions), order = DETECTOR_ORDER),
                    TextRecognitionComponentContribution(MangaOcrRecognizer(sessions), order = MANGA_OCR_ORDER),
                ),
                presets = listOf(
                    TextRecognitionPresetContribution(
                        OnnxTextRecognitionCatalog.japaneseMangaPreset,
                        order = JAPANESE_MANGA_PRESET_ORDER,
                    ),
                ),
            )
        }
    }

private const val DETECTOR_ORDER = 100
private const val MANGA_OCR_ORDER = 110
private const val JAPANESE_MANGA_PRESET_ORDER = 100
