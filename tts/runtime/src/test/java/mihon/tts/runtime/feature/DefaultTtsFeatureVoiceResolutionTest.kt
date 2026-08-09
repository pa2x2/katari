package mihon.tts.runtime.feature

import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import mihon.language.api.tag.LanguageTag
import mihon.tts.api.engine.KnownTtsEngine
import mihon.tts.api.engine.TtsEngineArtwork
import mihon.tts.api.engine.TtsEngineBuildAvailability
import mihon.tts.api.engine.TtsEngineDetails
import mihon.tts.api.engine.TtsEngineId
import mihon.tts.api.engine.TtsProviderId
import mihon.tts.api.preparation.TtsPreparation
import mihon.tts.api.preparation.TtsUnavailableReason
import mihon.tts.api.preparation.TtsVoiceChoiceReason
import mihon.tts.api.provider.TtsInputLimit
import mihon.tts.api.provider.TtsOptionalCapability
import mihon.tts.api.provider.TtsParameterRange
import mihon.tts.api.provider.TtsParameterSupport
import mihon.tts.api.provider.TtsProviderCapabilities
import mihon.tts.api.provider.TtsProviderPresentation
import mihon.tts.api.provider.TtsVoiceProcessing
import mihon.tts.api.request.ResolvedTtsRequest
import mihon.tts.api.request.TtsLanguageSelection
import mihon.tts.api.request.TtsParameters
import mihon.tts.api.request.TtsRequest
import mihon.tts.api.voice.TtsDefaultVoiceSelection
import mihon.tts.api.voice.TtsVoice
import mihon.tts.api.voice.TtsVoiceId
import mihon.tts.api.voice.TtsVoiceInspection
import mihon.tts.runtime.audio.TtsAudioFocus
import mihon.tts.runtime.preference.ProfileTtsPreferences
import mihon.tts.spi.engine.KnownTtsEngineCatalog
import mihon.tts.spi.engine.ReadyTtsEngineRequest
import mihon.tts.spi.engine.TtsEngine
import mihon.tts.spi.engine.TtsEngineDeviceAvailability
import mihon.tts.spi.engine.TtsEngineExecution
import mihon.tts.spi.engine.TtsEnginePreparation
import mihon.tts.spi.engine.TtsEngineRegistry
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.InMemoryPreferenceStore

class DefaultTtsFeatureVoiceResolutionTest {

    @Test
    fun `incompatible profile default falls back to a compatible local voice`() = runBlocking<Unit> {
        val preferences = ProfileTtsPreferences(InMemoryPreferenceStore(), ENGINE_ID).apply {
            setDefaultVoice(TtsDefaultVoiceSelection.Explicit(ENGLISH_NETWORK.id))
        }
        val engine = FakeEngine(
            voices = listOf(ENGLISH_NETWORK, RUSSIAN_NETWORK, RUSSIAN_LOCAL),
            defaultVoice = ENGLISH_NETWORK.id,
        )
        val feature = feature(engine, preferences)

        val preparation = feature.prepare(
            TtsRequest(
                text = "Башня надежды",
                language = TtsLanguageSelection.Explicit(RUSSIAN),
            ),
        ).shouldBeInstanceOf<TtsPreparation.Ready>()

        preparation.request.voice shouldBe RUSSIAN_LOCAL
        preparation.request.networkProcessingAllowed shouldBe false
        engine.preparedRequest shouldBe preparation.request
    }

    @Test
    fun `automatic fallback does not consent to a network-only compatible voice`() = runBlocking<Unit> {
        val preferences = ProfileTtsPreferences(InMemoryPreferenceStore(), ENGINE_ID)
        val feature = feature(
            engine = FakeEngine(
                voices = listOf(RUSSIAN_NETWORK),
                defaultVoice = RUSSIAN_NETWORK.id,
            ),
            preferences = preferences,
        )

        feature.prepare(
            TtsRequest(
                text = "Башня надежды",
                language = TtsLanguageSelection.Explicit(RUSSIAN),
            ),
        ) shouldBe TtsPreparation.Unavailable(
            TtsUnavailableReason.NetworkVoiceProhibited(RUSSIAN_NETWORK.id),
        )
    }

    @Test
    fun `profile network opt-in permits the selected network voice`() = runBlocking<Unit> {
        val preferences = ProfileTtsPreferences(InMemoryPreferenceStore(), ENGINE_ID).apply {
            setDefaultVoice(TtsDefaultVoiceSelection.Explicit(RUSSIAN_NETWORK.id))
            allowNetworkVoices.set(true)
        }
        val feature = feature(
            engine = FakeEngine(
                voices = listOf(RUSSIAN_NETWORK, RUSSIAN_LOCAL),
                defaultVoice = RUSSIAN_LOCAL.id,
            ),
            preferences = preferences,
        )

        val preparation = feature.prepare(
            TtsRequest(
                text = "Башня надежды",
                language = TtsLanguageSelection.Explicit(RUSSIAN),
            ),
        ).shouldBeInstanceOf<TtsPreparation.Ready>()

        preparation.request.voice shouldBe RUSSIAN_NETWORK
        preparation.request.networkProcessingAllowed shouldBe true
    }

    @Test
    fun `selected network voice remains prohibited without profile opt-in`() = runBlocking<Unit> {
        val preferences = ProfileTtsPreferences(InMemoryPreferenceStore(), ENGINE_ID).apply {
            setDefaultVoice(TtsDefaultVoiceSelection.Explicit(RUSSIAN_NETWORK.id))
        }
        val feature = feature(
            engine = FakeEngine(
                voices = listOf(RUSSIAN_NETWORK, RUSSIAN_LOCAL),
                defaultVoice = RUSSIAN_LOCAL.id,
            ),
            preferences = preferences,
        )

        feature.prepare(
            TtsRequest(
                text = "Башня надежды",
                language = TtsLanguageSelection.Explicit(RUSSIAN),
            ),
        ) shouldBe TtsPreparation.Unavailable(
            TtsUnavailableReason.NetworkVoiceProhibited(RUSSIAN_NETWORK.id),
        )
    }

    @Test
    fun `language without a compatible voice reports unsupported language`() = runBlocking<Unit> {
        val preferences = ProfileTtsPreferences(InMemoryPreferenceStore(), ENGINE_ID)
        val feature = feature(
            engine = FakeEngine(
                voices = listOf(ENGLISH_LOCAL),
                defaultVoice = ENGLISH_LOCAL.id,
            ),
            preferences = preferences,
        )

        feature.prepare(
            TtsRequest(
                text = "Башня надежды",
                language = TtsLanguageSelection.Explicit(RUSSIAN),
            ),
        ) shouldBe TtsPreparation.Unavailable(TtsUnavailableReason.UnsupportedLanguage(RUSSIAN))
    }

    @Test
    fun `disappeared profile voice remains selected and requests replacement`() = runBlocking<Unit> {
        val preferences = ProfileTtsPreferences(InMemoryPreferenceStore(), ENGINE_ID).apply {
            setDefaultVoice(TtsDefaultVoiceSelection.Explicit(RUSSIAN_NETWORK.id))
        }
        val feature = feature(
            engine = FakeEngine(
                voices = listOf(RUSSIAN_LOCAL),
                defaultVoice = RUSSIAN_LOCAL.id,
            ),
            preferences = preferences,
        )

        feature.prepare(
            TtsRequest(
                text = "Башня надежды",
                language = TtsLanguageSelection.Explicit(RUSSIAN),
            ),
        ) shouldBe TtsPreparation.VoiceChoiceRequired(
            engine = ENGINE_ID,
            language = RUSSIAN,
            reason = TtsVoiceChoiceReason.SelectedVoiceUnavailable(RUSSIAN_NETWORK.id),
            voices = listOf(RUSSIAN_LOCAL),
        )
    }

    @Test
    fun `profile speech parameters reach provider preparation`() = runBlocking<Unit> {
        val preferences = ProfileTtsPreferences(InMemoryPreferenceStore(), ENGINE_ID).apply {
            speechRate.set(1.25f)
            pitch.set(1.1f)
        }
        val engine = FakeEngine(
            voices = listOf(ENGLISH_LOCAL),
            defaultVoice = ENGLISH_LOCAL.id,
            capabilities = PARAMETER_CAPABILITIES,
        )
        val feature = feature(engine, preferences)

        val preparation = feature.prepare(
            TtsRequest(
                text = "Tower of hope",
                language = TtsLanguageSelection.Explicit(ENGLISH),
            ),
        ).shouldBeInstanceOf<TtsPreparation.Ready>()

        preparation.request.parameters shouldBe TtsParameters(speechRate = 1.25f, pitch = 1.1f)
        engine.preparedRequest shouldBe preparation.request
    }

    @Test
    fun `provider initialization error is sanitized as engine unavailability`() = runBlocking<Unit> {
        val preferences = ProfileTtsPreferences(InMemoryPreferenceStore(), ENGINE_ID)
        val feature = feature(
            engine = FakeEngine(
                voices = listOf(ENGLISH_LOCAL),
                defaultVoice = ENGLISH_LOCAL.id,
                prepareFailure = IllegalStateException("private selected text"),
            ),
            preferences = preferences,
        )

        feature.prepare(
            TtsRequest(
                text = "private selected text",
                language = TtsLanguageSelection.Explicit(ENGLISH),
            ),
        ) shouldBe TtsPreparation.Unavailable(
            TtsUnavailableReason.EngineUnavailable(ENGINE_ID, "TTS engine preparation failed"),
        )
    }

    private fun CoroutineScope.feature(
        engine: FakeEngine,
        preferences: ProfileTtsPreferences,
    ) = DefaultTtsFeature(
        engineRegistry = object : TtsEngineRegistry {
            override val engines: List<TtsEngine> = listOf(engine)

            override fun find(engine: TtsEngineId): TtsEngine? = engines.singleOrNull {
                it.catalogEntry.id == engine
            }
        },
        knownEngineCatalog = object : KnownTtsEngineCatalog {
            override val knownEngines: List<KnownTtsEngine> = listOf(ENGINE)
        },
        textLanguageDetectors = emptyList(),
        preferences = preferences,
        selectedEngine = { ENGINE_ID },
        scope = this,
        audioFocus = object : TtsAudioFocus {
            override fun request() = true

            override fun abandon() = Unit
        },
    )

    private class FakeEngine(
        private val voices: List<TtsVoice>,
        private val defaultVoice: TtsVoiceId?,
        override val capabilities: TtsProviderCapabilities = CAPABILITIES,
        private val prepareFailure: RuntimeException? = null,
    ) : TtsEngine {
        override val catalogEntry = ENGINE
        override val presentation = PRESENTATION
        var preparedRequest: ResolvedTtsRequest? = null

        override suspend fun inspectDevice() = TtsEngineDeviceAvailability.Available

        override suspend fun inspectVoices() = TtsVoiceInspection.Available(
            engine = ENGINE_ID,
            voices = voices,
            defaultVoice = defaultVoice,
        )

        override suspend fun prepare(request: ResolvedTtsRequest): TtsEnginePreparation.Ready {
            prepareFailure?.let { throw it }
            preparedRequest = request
            return TtsEnginePreparation.Ready(READY_REQUEST)
        }

        override suspend fun revalidate(ready: ReadyTtsEngineRequest) =
            TtsEnginePreparation.Ready(READY_REQUEST)

        override suspend fun play(ready: ReadyTtsEngineRequest) = TtsEngineExecution.Failed("Unused")
    }

    private companion object {
        val PROVIDER_ID = TtsProviderId("test")
        val ENGINE_ID = TtsEngineId("test-engine")
        val ENGLISH = LanguageTag.require("en-US")
        val RUSSIAN = LanguageTag.require("ru-RU")
        val ENGINE = KnownTtsEngine(
            id = ENGINE_ID,
            providerId = PROVIDER_ID,
            providerName = "Test provider",
            engineName = "Test engine",
            buildAvailability = TtsEngineBuildAvailability.Included,
            artwork = TtsEngineArtwork.Bundled(1),
            details = TtsEngineDetails(
                description = "Test engine",
                processingDescription = "Test processing",
                privacyDescription = "Test privacy",
            ),
        )
        val PRESENTATION = TtsProviderPresentation(
            providerId = PROVIDER_ID,
            providerName = "Test provider",
            engineName = "Test engine",
        )
        val CAPABILITIES = TtsProviderCapabilities(
            rangeProgress = TtsOptionalCapability.Unsupported,
            speechRate = TtsParameterSupport.Unsupported,
            pitch = TtsParameterSupport.Unsupported,
            inputLimit = TtsInputLimit.Unspecified,
        )
        val ENGLISH_NETWORK = voice("english-network", ENGLISH, TtsVoiceProcessing.NetworkRequired)
        val ENGLISH_LOCAL = voice("english-local", ENGLISH, TtsVoiceProcessing.OnDevice)
        val RUSSIAN_NETWORK = voice("russian-network", RUSSIAN, TtsVoiceProcessing.NetworkRequired)
        val RUSSIAN_LOCAL = voice("russian-local", RUSSIAN, TtsVoiceProcessing.OnDevice)
        val READY_REQUEST = object : ReadyTtsEngineRequest {}
        val PARAMETER_CAPABILITIES = CAPABILITIES.copy(
            speechRate = TtsParameterSupport.Supported(TtsParameterRange(0.5f, 2f, 1f)),
            pitch = TtsParameterSupport.Supported(TtsParameterRange(0.5f, 2f, 1f)),
        )

        fun voice(
            name: String,
            language: LanguageTag,
            processing: TtsVoiceProcessing,
        ) = TtsVoice(
            id = TtsVoiceId(PROVIDER_ID, ENGINE_ID, name),
            name = name,
            language = language,
            processing = processing,
        )
    }
}
