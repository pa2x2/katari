package app.katari.text.recognition.provider.onnx

import android.app.Application
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
        return onnxTextRecognitionContribution(sessions)
    }
}
