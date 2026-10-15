package mihon.text.recognition.provider.onnx

import android.app.Application
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import mihon.text.recognition.provider.onnx.catalog.OnnxTextRecognitionCatalog
import mihon.text.recognition.provider.onnx.detector.ComicTextDetector
import mihon.text.recognition.provider.onnx.paddle.PaddleOcrRecognizer
import mihon.text.recognition.provider.onnx.recognizer.MangaOcrRecognizer
import mihon.text.recognition.provider.onnx.session.OnnxSessionMemoryRelease
import mihon.text.recognition.provider.onnx.session.OnnxSessions
import mihon.text.recognition.spi.contribution.TextRecognitionProviderContribution

@ContributesTo(AppScope::class)
@BindingContainer
object OnnxTextRecognitionBindings {
    @Provides
    @IntoSet
    fun provider(application: Application): TextRecognitionProviderContribution {
        val sessions = OnnxSessions()
        application.registerComponentCallbacks(OnnxSessionMemoryRelease(sessions))
        return TextRecognitionProviderContribution(
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
    }
}

private const val ONNX_PROVIDER_ORDER = 100
