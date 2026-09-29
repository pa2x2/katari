package mihon.text.recognition.provider.onnx

import android.app.Application
import mihon.feature.runtime.application.ApplicationFeatureRuntimeComponent
import mihon.text.recognition.provider.onnx.catalog.OnnxTextRecognitionCatalog
import mihon.text.recognition.provider.onnx.detector.ComicTextDetector
import mihon.text.recognition.provider.onnx.paddle.PaddleOcrRecognizer
import mihon.text.recognition.provider.onnx.recognizer.MangaOcrRecognizer
import mihon.text.recognition.provider.onnx.session.OnnxSessionMemoryRelease
import mihon.text.recognition.provider.onnx.session.OnnxSessions
import mihon.text.recognition.runtime.component.TextRecognitionRuntimeComponent
import mihon.text.recognition.runtime.component.TextRecognitionRuntimeContribution
import mihon.text.recognition.spi.contribution.TextRecognitionProviderContribution

val onnxTextRecognitionRuntimeComponent: ApplicationFeatureRuntimeComponent =
    object : TextRecognitionRuntimeComponent {
        override fun contribute(application: Application): TextRecognitionRuntimeContribution {
            val sessions = OnnxSessions()
            application.registerComponentCallbacks(OnnxSessionMemoryRelease(sessions))
            return TextRecognitionRuntimeContribution(
                providers = listOf(
                    TextRecognitionProviderContribution(
                        provider = OnnxTextRecognitionCatalog.provider,
                        components = OnnxTextRecognitionCatalog.components,
                        implementations = listOf(
                            ComicTextDetector(sessions),
                            MangaOcrRecognizer(sessions),
                            PaddleOcrRecognizer(sessions),
                        ),
                        presets = OnnxTextRecognitionCatalog.presets,
                        order = ONNX_PROVIDER_ORDER,
                    ),
                ),
            )
        }
    }

private const val ONNX_PROVIDER_ORDER = 100
