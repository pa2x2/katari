package app.katari.tts.ui.settings

import app.katari.language.api.tag.LanguageTag
import app.katari.tts.api.TtsFeature
import app.katari.tts.api.engine.TtsEngineId
import app.katari.tts.api.engine.TtsProviderId
import app.katari.tts.api.playback.TtsPlaybackSession
import app.katari.tts.api.playback.TtsPlaybackStart
import app.katari.tts.api.preparation.ReadyTts
import app.katari.tts.api.preparation.TtsPreparation
import app.katari.tts.api.provider.TtsProviderPresentation
import app.katari.tts.api.provider.TtsVoiceProcessing
import app.katari.tts.api.request.ResolvedTtsRequest
import app.katari.tts.api.request.TtsLanguageSelection
import app.katari.tts.api.request.TtsParameters
import app.katari.tts.api.request.TtsRequest
import app.katari.tts.api.voice.TtsVoice
import app.katari.tts.api.voice.TtsVoiceId

/** Prepares every explicit-language request with an English test voice and starts [sessions] in order. */
internal class TestTtsFeature(sessions: List<TtsPlaybackSession>) : TtsFeature {
    private val remainingSessions = ArrayDeque(sessions)

    override suspend fun prepare(request: TtsRequest): TtsPreparation {
        val language = (request.language as TtsLanguageSelection.Explicit).language
        return TtsPreparation.Ready(
            speech = TestReadyTts,
            request = ResolvedTtsRequest(
                text = request.text,
                language = language,
                engine = ENGINE,
                voice = ENGLISH_VOICE,
                parameters = TtsParameters(),
                networkProcessingAllowed = false,
            ),
            presentation = TtsProviderPresentation(
                providerId = PROVIDER,
                providerName = "Test provider",
                engineName = "Test engine",
            ),
        )
    }

    override suspend fun play(ready: ReadyTts) = TtsPlaybackStart.Started(remainingSessions.removeFirst())
}

internal val ENGLISH = LanguageTag.require("en-US")
private val PROVIDER = TtsProviderId("test-provider")
private val ENGINE = TtsEngineId("test-engine")

private val ENGLISH_VOICE = TtsVoice(
    id = TtsVoiceId(PROVIDER, ENGINE, "en-local"),
    name = "English local",
    language = ENGLISH,
    processing = TtsVoiceProcessing.OnDevice,
)

private object TestReadyTts : ReadyTts
