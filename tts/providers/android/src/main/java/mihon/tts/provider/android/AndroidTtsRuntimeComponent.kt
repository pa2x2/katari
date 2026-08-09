package mihon.tts.provider.android

import android.app.Application
import mihon.feature.runtime.application.ApplicationFeatureRuntimeComponent
import mihon.tts.provider.android.discovery.discoverAndroidTtsEngines
import mihon.tts.runtime.component.TtsRuntimeComponent
import mihon.tts.runtime.component.TtsRuntimeContribution

val androidTtsRuntimeComponent: ApplicationFeatureRuntimeComponent =
    object : TtsRuntimeComponent {
        override fun contribute(application: Application): TtsRuntimeContribution {
            return TtsRuntimeContribution(
                engineContributions = discoverAndroidTtsEngines(application),
            )
        }
    }
