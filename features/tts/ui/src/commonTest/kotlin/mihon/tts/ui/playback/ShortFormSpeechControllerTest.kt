package mihon.tts.ui.playback

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import mihon.language.api.tag.LanguageTag
import mihon.tts.api.TtsFeature
import mihon.tts.api.engine.TtsEngineId
import mihon.tts.api.playback.TtsPlaybackStart
import mihon.tts.api.preparation.ReadyTts
import mihon.tts.api.preparation.TtsPreparation
import mihon.tts.api.preparation.TtsUnavailableReason
import mihon.tts.api.request.TtsLanguageSelection
import mihon.tts.api.request.TtsRequest
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ShortFormSpeechControllerTest {
    @Test
    fun `an engine that can't speak says why in its own words`() = runTest {
        val failures = mutableListOf<ShortFormSpeechFailure>()
        val controller = ShortFormSpeechController<String>(UnavailableFeature, backgroundScope, failures::add)

        controller.toggle(
            ShortFormSpeechRequest("selection", "Hello", TtsLanguageSelection.Explicit(LanguageTag.require("en"))),
        )
        runCurrent()

        failures shouldBe listOf(ShortFormSpeechFailure.Unavailable(NOT_INSTALLED))
    }

    private object UnavailableFeature : TtsFeature {
        override suspend fun prepare(request: TtsRequest): TtsPreparation = TtsPreparation.Unavailable(
            TtsUnavailableReason.EngineUnavailable(TtsEngineId("speech-dispatcher"), NOT_INSTALLED),
        )

        override suspend fun play(ready: ReadyTts): TtsPlaybackStart = error("Nothing is ready to play")
    }

    private companion object {
        const val NOT_INSTALLED = "Speech Dispatcher isn't installed."
    }
}
