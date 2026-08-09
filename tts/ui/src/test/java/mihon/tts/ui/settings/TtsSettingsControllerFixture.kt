package mihon.tts.ui.settings

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import mihon.language.api.tag.LanguageTag
import mihon.tts.api.TtsFeature
import mihon.tts.api.availability.TtsDeviceAvailability
import mihon.tts.api.engine.KnownTtsEngine
import mihon.tts.api.engine.TtsEngineArtwork
import mihon.tts.api.engine.TtsEngineBuildAvailability
import mihon.tts.api.engine.TtsEngineDetails
import mihon.tts.api.engine.TtsEngineId
import mihon.tts.api.engine.TtsEngineInspection
import mihon.tts.api.engine.TtsEngineState
import mihon.tts.api.engine.TtsEngineStatus
import mihon.tts.api.engine.TtsProviderId
import mihon.tts.api.host.TtsHostActionResult
import mihon.tts.api.host.TtsHostActions
import mihon.tts.api.playback.TtsPlaybackSession
import mihon.tts.api.playback.TtsPlaybackStart
import mihon.tts.api.playback.TtsPlaybackState
import mihon.tts.api.playback.TtsStopReason
import mihon.tts.api.playback.TtsStopResult
import mihon.tts.api.preparation.ReadyTts
import mihon.tts.api.preparation.TtsPreparation
import mihon.tts.api.provider.TtsInputLimit
import mihon.tts.api.provider.TtsOptionalCapability
import mihon.tts.api.provider.TtsParameterRange
import mihon.tts.api.provider.TtsParameterSupport
import mihon.tts.api.provider.TtsProviderCapabilities
import mihon.tts.api.provider.TtsProviderDisclosure
import mihon.tts.api.provider.TtsProviderPresentation
import mihon.tts.api.provider.TtsVoiceProcessing
import mihon.tts.api.request.ResolvedTtsRequest
import mihon.tts.api.request.TtsLanguageSelection
import mihon.tts.api.request.TtsParameters
import mihon.tts.api.request.TtsRequest
import mihon.tts.api.voice.TtsVoice
import mihon.tts.api.voice.TtsVoiceId
import mihon.tts.api.voice.TtsVoiceInspection
import tachiyomi.core.common.preference.InMemoryPreferenceStore.InMemoryPreference
import tachiyomi.core.common.preference.Preference

internal class TestTtsHostActions(
    val engineStates: List<TtsEngineState> = TEST_ENGINE_STATES,
    val voices: List<TtsVoice> = TEST_VOICES,
) : TtsHostActions {
    override val knownEngines = engineStates.map(TtsEngineState::engine)
    override val selectedEngine: Preference<TtsEngineId> = InMemoryPreference("engine", FIRST_ENGINE, FIRST_ENGINE)
    override val speechRate: Preference<Float> = InMemoryPreference("rate", null, 1f)
    override val pitch: Preference<Float> = InMemoryPreference("pitch", null, 1f)
    override val allowNetworkVoices: Preference<Boolean> = InMemoryPreference("network", null, false)
    private val voiceOverrides = mutableMapOf<LanguageTag, TtsVoiceId>()

    override suspend fun deviceAvailability() = TtsDeviceAvailability.Available

    override suspend fun inspectEngines() = TtsEngineInspection(
        engines = engineStates,
        selectedEngine = selectedEngine.get(),
    )

    override suspend fun inspectVoices(engine: TtsEngineId) = TtsVoiceInspection.Available(engine, voices)

    override suspend fun acknowledgeProviderDisclosure(
        engine: TtsEngineId,
        disclosure: TtsProviderDisclosure,
    ) = TtsHostActionResult.Completed

    override fun supportsSetup(engine: TtsEngineId) = false

    override suspend fun openSetup(engine: TtsEngineId) = TtsHostActionResult.SetupUnsupported

    override suspend fun installVoiceData(
        engine: TtsEngineId,
        languages: Set<LanguageTag>,
    ) = TtsHostActionResult.SetupUnsupported

    override fun selectedVoice(language: LanguageTag): TtsVoiceId? = voiceOverrides[language]

    override fun selectedVoiceOverrides(): Map<LanguageTag, TtsVoiceId> = voiceOverrides.toMap()

    override fun setSelectedEngine(engine: TtsEngineId) {
        selectedEngine.set(engine)
    }

    override fun setSelectedVoice(language: LanguageTag, voice: TtsVoiceId?) {
        if (voice == null) {
            voiceOverrides.remove(language)
        } else {
            voiceOverrides[language] = voice
        }
    }
}

internal class TestTtsFeature : TtsFeature {
    val preparedRequests = mutableListOf<TtsRequest>()
    val session = TestTtsPlaybackSession()

    override suspend fun prepare(request: TtsRequest): TtsPreparation {
        preparedRequests += request
        val language = (request.language as TtsLanguageSelection.Explicit).language
        return TtsPreparation.Ready(
            speech = TestReadyTts,
            request = ResolvedTtsRequest(
                text = request.text,
                language = language,
                engine = FIRST_ENGINE,
                voice = TEST_VOICES.compatibleWith(language).first(),
                parameters = TtsParameters(),
                networkProcessingAllowed = false,
            ),
            presentation = TEST_PRESENTATION,
        )
    }

    override suspend fun play(ready: ReadyTts) = TtsPlaybackStart.Started(session)
}

internal class TestTtsPlaybackSession : TtsPlaybackSession {
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

internal val ENGLISH = LanguageTag.require("en-US")
internal val PORTUGUESE_BRAZIL = LanguageTag.require("pt-BR")
internal val PORTUGUESE_PORTUGAL = LanguageTag.require("pt-PT")
internal val PROVIDER = TtsProviderId("test-provider")
internal val FIRST_ENGINE = TtsEngineId("first-engine")
internal val SECOND_ENGINE = TtsEngineId("second-engine")
internal val BLOCKED_ENGINE = TtsEngineId("blocked-engine")

internal val PORTUGUESE_LOCAL_VOICE = TtsVoice(
    id = TtsVoiceId(PROVIDER, FIRST_ENGINE, "pt-local"),
    name = "Portuguese local",
    language = PORTUGUESE_PORTUGAL,
    processing = TtsVoiceProcessing.OnDevice,
)
internal val PORTUGUESE_NETWORK_VOICE = TtsVoice(
    id = TtsVoiceId(PROVIDER, FIRST_ENGINE, "pt-network"),
    name = "Portuguese network",
    language = PORTUGUESE_BRAZIL,
    processing = TtsVoiceProcessing.NetworkRequired,
)
internal val ENGLISH_VOICE = TtsVoice(
    id = TtsVoiceId(PROVIDER, FIRST_ENGINE, "en-local"),
    name = "English local",
    language = ENGLISH,
    processing = TtsVoiceProcessing.OnDevice,
)
internal val TEST_VOICES = listOf(PORTUGUESE_NETWORK_VOICE, ENGLISH_VOICE, PORTUGUESE_LOCAL_VOICE)

private val TEST_CAPABILITIES = TtsProviderCapabilities(
    rangeProgress = TtsOptionalCapability.Unsupported,
    speechRate = TtsParameterSupport.Supported(TtsParameterRange(0.5f, 2f, 1f)),
    pitch = TtsParameterSupport.Supported(TtsParameterRange(0.5f, 2f, 1f)),
    inputLimit = TtsInputLimit.Unspecified,
)
private val TEST_ENGINE_DETAILS = TtsEngineDetails(
    description = "Test engine",
    processingDescription = "Test processing",
    privacyDescription = "Test privacy",
)

private fun engine(id: TtsEngineId) = KnownTtsEngine(
    id = id,
    providerId = PROVIDER,
    providerName = "Test provider",
    engineName = id.value,
    buildAvailability = TtsEngineBuildAvailability.Included,
    artwork = TtsEngineArtwork.Bundled(android.R.drawable.sym_def_app_icon),
    details = TEST_ENGINE_DETAILS,
)

private val TEST_PRESENTATION = TtsProviderPresentation(
    providerId = PROVIDER,
    providerName = "Test provider",
    engineName = "Test engine",
)

private val TEST_ENGINE_STATES = listOf(
    TtsEngineState(engine(FIRST_ENGINE), TEST_PRESENTATION, TtsEngineStatus.Ready, capabilities = TEST_CAPABILITIES),
    TtsEngineState(engine(SECOND_ENGINE), TEST_PRESENTATION, TtsEngineStatus.Ready, capabilities = TEST_CAPABILITIES),
    TtsEngineState(
        engine(BLOCKED_ENGINE),
        TEST_PRESENTATION,
        TtsEngineStatus.Unavailable("Unavailable in fixture"),
        capabilities = TEST_CAPABILITIES,
    ),
)

private object TestReadyTts : ReadyTts
