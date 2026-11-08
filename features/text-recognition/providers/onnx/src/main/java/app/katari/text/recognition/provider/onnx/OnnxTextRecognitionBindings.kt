package app.katari.text.recognition.provider.onnx

import android.app.Application
import app.katari.text.recognition.provider.onnx.catalog.OnnxTextRecognitionCatalog
import app.katari.text.recognition.provider.onnx.detector.ComicTextDetector
import app.katari.text.recognition.provider.onnx.paddle.PaddleOcrRecognizer
import app.katari.text.recognition.provider.onnx.recognizer.MangaOcrRecognizer
import app.katari.text.recognition.provider.onnx.session.OnnxSessionMemoryRelease
import app.katari.text.recognition.provider.onnx.session.OnnxSessions
import app.katari.text.recognition.spi.contribution.TextRecognitionProviderContribution
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides

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
