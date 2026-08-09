package mihon.entry.interactions.book.reader.speech

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import mihon.language.api.tag.LanguageTag
import mihon.translation.ui.presentation.TranslationResultSpeechSide
import mihon.translation.ui.presentation.TranslationResultSpeechTarget
import mihon.tts.api.TtsFeature
import mihon.tts.api.engine.TtsEngineId
import mihon.tts.api.engine.TtsProviderId
import mihon.tts.api.playback.TtsPlaybackSession
import mihon.tts.api.playback.TtsPlaybackStart
import mihon.tts.api.playback.TtsPlaybackState
import mihon.tts.api.playback.TtsStopReason
import mihon.tts.api.playback.TtsStopResult
import mihon.tts.api.preparation.ReadyTts
import mihon.tts.api.preparation.TtsLanguageChoiceReason
import mihon.tts.api.preparation.TtsPreparation
import mihon.tts.api.provider.TtsProviderPresentation
import mihon.tts.api.provider.TtsVoiceProcessing
import mihon.tts.api.request.ResolvedTtsRequest
import mihon.tts.api.request.TtsLanguageSelection
import mihon.tts.api.request.TtsParameters
import mihon.tts.api.request.TtsRequest
import mihon.tts.api.voice.TtsVoice
import mihon.tts.api.voice.TtsVoiceId
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BookShortFormSpeechControllerTest {

    @Test
    fun `late obsolete preparation cannot replace the current speech owner`() = runTest {
        val feature = DeferredFirstPreparationFeature()
        val failures = mutableListOf<BookShortFormSpeechFailure>()
        val controller = BookShortFormSpeechController(feature, backgroundScope, failures::add)
        val selection = request(BookShortFormSpeechOwner.Selection("selection"), "first")
        val translationTarget = TranslationResultSpeechTarget(
            side = TranslationResultSpeechSide.Target,
            text = "second",
            language = ENGLISH,
        )
        val translation = request(
            BookShortFormSpeechOwner.TranslationResult(translationTarget),
            translationTarget.text,
        )

        controller.toggle(selection)
        runCurrent()
        controller.toggle(translation)
        runCurrent()

        feature.playedTexts shouldContainExactly listOf("second")
        controller.state.value shouldBe BookShortFormSpeechState(
            phase = BookShortFormSpeechPhase.Speaking,
            owner = translation.owner,
        )

        feature.releaseFirstPreparation.complete(Unit)
        runCurrent()

        feature.playedTexts shouldContainExactly listOf("second")
        failures shouldBe emptyList()
        controller.close()
        runCurrent()
        feature.sessions.getValue("second").stopCount shouldBe 1
    }

    @Test
    fun `ambiguous selection language returns to idle without starting playback`() = runTest {
        val failures = mutableListOf<BookShortFormSpeechFailure>()
        val feature = FixedPreparationFeature(
            TtsPreparation.LanguageChoiceRequired(
                reason = TtsLanguageChoiceReason.Ambiguous,
                suggestedLanguages = listOf(ENGLISH),
            ),
        )
        val controller = BookShortFormSpeechController(feature, backgroundScope, failures::add)

        controller.toggle(request(BookShortFormSpeechOwner.Selection("selection"), "ambiguous"))
        runCurrent()

        controller.state.value shouldBe BookShortFormSpeechState()
        failures shouldContainExactly listOf(BookShortFormSpeechFailure.LanguageUnavailable)
        feature.playCount shouldBe 0
        controller.close()
    }

    private fun request(owner: BookShortFormSpeechOwner, text: String) = BookShortFormSpeechRequest(
        owner = owner,
        text = text,
        language = TtsLanguageSelection.Explicit(ENGLISH),
    )

    private class DeferredFirstPreparationFeature : TtsFeature {
        val releaseFirstPreparation = CompletableDeferred<Unit>()
        val playedTexts = mutableListOf<String>()
        val sessions = mutableMapOf<String, RecordingSession>()

        override suspend fun prepare(request: TtsRequest): TtsPreparation {
            if (request.text == "first") {
                withContext(NonCancellable) { releaseFirstPreparation.await() }
            }
            return ready(request)
        }

        override suspend fun play(ready: ReadyTts): TtsPlaybackStart {
            val text = (ready as TestReadyTts).text
            playedTexts += text
            return TtsPlaybackStart.Started(sessions.getOrPut(text, ::RecordingSession))
        }
    }

    private class FixedPreparationFeature(
        private val preparation: TtsPreparation,
    ) : TtsFeature {
        var playCount = 0
            private set

        override suspend fun prepare(request: TtsRequest) = preparation

        override suspend fun play(ready: ReadyTts): TtsPlaybackStart {
            playCount += 1
            return TtsPlaybackStart.Started(RecordingSession())
        }
    }

    private class RecordingSession : TtsPlaybackSession {
        private val mutableState = MutableStateFlow<TtsPlaybackState>(TtsPlaybackState.Speaking())
        override val state: StateFlow<TtsPlaybackState> = mutableState
        var stopCount = 0
            private set

        override suspend fun stop(): TtsStopResult {
            stopCount += 1
            mutableState.value = TtsPlaybackState.Stopped(TtsStopReason.Requested)
            return TtsStopResult.Stopped
        }
    }

    private companion object {
        val ENGLISH = LanguageTag.require("en-US")
        val PROVIDER = TtsProviderId("test")
        val ENGINE = TtsEngineId("test-engine")
        val VOICE = TtsVoice(
            id = TtsVoiceId(PROVIDER, ENGINE, "voice"),
            name = "Voice",
            language = ENGLISH,
            processing = TtsVoiceProcessing.OnDevice,
        )
        val PRESENTATION = TtsProviderPresentation(PROVIDER, "Test provider", "Test engine")

        fun ready(request: TtsRequest): TtsPreparation.Ready {
            val language = (request.language as TtsLanguageSelection.Explicit).language
            return TtsPreparation.Ready(
                speech = TestReadyTts(request.text),
                request = ResolvedTtsRequest(
                    text = request.text,
                    language = language,
                    engine = ENGINE,
                    voice = VOICE,
                    parameters = TtsParameters(),
                    networkProcessingAllowed = false,
                ),
                presentation = PRESENTATION,
            )
        }
    }

    private data class TestReadyTts(val text: String) : ReadyTts
}
