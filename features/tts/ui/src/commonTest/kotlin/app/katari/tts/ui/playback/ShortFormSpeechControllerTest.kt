package app.katari.tts.ui.playback

import app.katari.language.api.tag.LanguageTag
import app.katari.tts.api.TtsFeature
import app.katari.tts.api.engine.TtsEngineId
import app.katari.tts.api.playback.TtsPlaybackStart
import app.katari.tts.api.preparation.ReadyTts
import app.katari.tts.api.preparation.TtsPreparation
import app.katari.tts.api.preparation.TtsUnavailableReason
import app.katari.tts.api.request.TtsLanguageSelection
import app.katari.tts.api.request.TtsRequest
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
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
