package mihon.tts.ui.settings

import mihon.language.api.tag.LanguageTag
import mihon.tts.api.TtsFeature
import mihon.tts.api.engine.TtsEngineId
import mihon.tts.api.engine.TtsProviderId
import mihon.tts.api.playback.TtsPlaybackSession
import mihon.tts.api.playback.TtsPlaybackStart
import mihon.tts.api.preparation.ReadyTts
import mihon.tts.api.preparation.TtsPreparation
import mihon.tts.api.provider.TtsProviderPresentation
import mihon.tts.api.provider.TtsVoiceProcessing
import mihon.tts.api.request.ResolvedTtsRequest
import mihon.tts.api.request.TtsLanguageSelection
import mihon.tts.api.request.TtsParameters
import mihon.tts.api.request.TtsRequest
import mihon.tts.api.voice.TtsVoice
import mihon.tts.api.voice.TtsVoiceId

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
