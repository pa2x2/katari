package mihon.tts.provider.android

import android.app.Application
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.ElementsIntoSet
import dev.zacsweers.metro.Provides
import mihon.tts.provider.android.discovery.discoverAndroidTtsEngines
import mihon.tts.spi.contribution.TtsEngineContribution

@ContributesTo(AppScope::class)
@BindingContainer
object AndroidTtsBindings {
    @Provides
    @ElementsIntoSet
    fun engines(application: Application): Set<TtsEngineContribution> =
        discoverAndroidTtsEngines(application).toSet()
}
