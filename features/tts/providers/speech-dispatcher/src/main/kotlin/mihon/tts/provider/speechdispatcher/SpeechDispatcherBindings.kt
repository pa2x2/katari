package mihon.tts.provider.speechdispatcher

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import mihon.tts.provider.speechdispatcher.engine.SPEECH_DISPATCHER_PROVIDER_ID
import mihon.tts.provider.speechdispatcher.engine.SpeechDispatcherEngine
import mihon.tts.provider.speechdispatcher.engine.speechDispatcherCatalogEntry
import mihon.tts.provider.speechdispatcher.engine.speechDispatcherPresentation
import mihon.tts.provider.speechdispatcher.service.SpeechDispatcherLauncher
import mihon.tts.provider.speechdispatcher.service.speechDispatcherAddress
import mihon.tts.spi.contribution.TtsEngineContribution
import mihon.tts.spi.contribution.TtsProviderContribution

@ContributesTo(AppScope::class)
@BindingContainer
object SpeechDispatcherBindings {
    @Provides
    @IntoSet
    fun provider(): TtsProviderContribution = TtsProviderContribution(SPEECH_DISPATCHER_PROVIDER_ID)

    @Provides
    @IntoSet
    fun engine(): TtsEngineContribution {
        val environment = System.getenv()
        val catalogEntry = speechDispatcherCatalogEntry()
        return TtsEngineContribution(
            engine = SpeechDispatcherEngine(
                catalogEntry = catalogEntry,
                presentation = catalogEntry.speechDispatcherPresentation(),
                address = speechDispatcherAddress(environment, home = System.getProperty("user.home")),
                launcher = SpeechDispatcherLauncher(environment),
                clientName = clientName(),
            ),
        )
    }

    /** `user:application:component`, the shape Speech Dispatcher's clients name themselves in. */
    private fun clientName(): String {
        val user = System.getProperty("user.name").orEmpty().filter { it.isLetterOrDigit() || it in "._-" }
        return "${user.ifEmpty { "user" }}:katari:speech"
    }
}
