package app.katari.tts.provider.android

import android.app.Application
import app.katari.tts.provider.android.discovery.ANDROID_TTS_PROVIDER_ID
import app.katari.tts.provider.android.discovery.discoverAndroidTtsEngines
import app.katari.tts.spi.contribution.TtsEngineContribution
import app.katari.tts.spi.contribution.TtsProviderContribution
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.ElementsIntoSet
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides

@ContributesTo(AppScope::class)
@BindingContainer
object AndroidTtsBindings {
    @Provides
    @IntoSet
    fun provider(): TtsProviderContribution = TtsProviderContribution(ANDROID_TTS_PROVIDER_ID)

    @Provides
    @ElementsIntoSet
    fun engines(application: Application): Set<TtsEngineContribution> =
        discoverAndroidTtsEngines(application).toSet()
}
