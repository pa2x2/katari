package app.katari.text.recognition.provider.onnx

import app.katari.text.recognition.provider.onnx.session.OnnxSessionIdleRelease
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
    fun provider(): TextRecognitionProviderContribution {
        lateinit var sessions: OnnxSessions
        val release = OnnxSessionIdleRelease { sessions.releaseAll() }
        sessions = OnnxSessions(release::onInUseChanged)
        return onnxTextRecognitionContribution(sessions)
    }
}
