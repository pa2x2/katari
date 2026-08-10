package mihon.tts.runtime.component

import android.app.Application
import mihon.feature.runtime.application.ApplicationFeatureRuntimeComponent
import mihon.tts.spi.contribution.TtsEngineContribution

interface TtsRuntimeComponent : ApplicationFeatureRuntimeComponent {
    fun contribute(application: Application): TtsRuntimeContribution
}

data class TtsRuntimeContribution(
    val engineContributions: List<TtsEngineContribution> = emptyList(),
)
