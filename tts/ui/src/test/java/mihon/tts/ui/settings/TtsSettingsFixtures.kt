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

/** Prepares every explicit-language request with a compatible test voice and starts [sessions] in order. */
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
                voice = TEST_VOICES.compatibleWith(language).first(),
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
internal val PORTUGUESE_BRAZIL = LanguageTag.require("pt-BR")
private val PORTUGUESE_PORTUGAL = LanguageTag.require("pt-PT")
private val PROVIDER = TtsProviderId("test-provider")
private val ENGINE = TtsEngineId("test-engine")

internal val PORTUGUESE_LOCAL_VOICE = TtsVoice(
    id = TtsVoiceId(PROVIDER, ENGINE, "pt-local"),
    name = "Portuguese local",
    language = PORTUGUESE_PORTUGAL,
    processing = TtsVoiceProcessing.OnDevice,
)
internal val PORTUGUESE_NETWORK_VOICE = TtsVoice(
    id = TtsVoiceId(PROVIDER, ENGINE, "pt-network"),
    name = "Portuguese network",
    language = PORTUGUESE_BRAZIL,
    processing = TtsVoiceProcessing.NetworkRequired,
)
private val ENGLISH_VOICE = TtsVoice(
    id = TtsVoiceId(PROVIDER, ENGINE, "en-local"),
    name = "English local",
    language = ENGLISH,
    processing = TtsVoiceProcessing.OnDevice,
)
internal val TEST_VOICES = listOf(PORTUGUESE_NETWORK_VOICE, ENGLISH_VOICE, PORTUGUESE_LOCAL_VOICE)

private object TestReadyTts : ReadyTts
