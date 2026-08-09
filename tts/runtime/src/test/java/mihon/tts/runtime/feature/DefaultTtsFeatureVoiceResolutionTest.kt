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
import mihon.tts.api.provider.TtsInputLimit
import mihon.tts.api.provider.TtsOptionalCapability
import mihon.tts.api.provider.TtsParameterSupport
import mihon.tts.api.provider.TtsProviderCapabilities
import mihon.tts.api.provider.TtsProviderPresentation
import mihon.tts.api.provider.TtsVoiceProcessing
import mihon.tts.api.request.ResolvedTtsRequest
import mihon.tts.api.request.TtsLanguageSelection
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
    fun `incompatible profile default falls back to a compatible local voice`() = runBlocking {
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
    fun `automatic fallback does not consent to a network-only compatible voice`() = runBlocking {
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
    fun `explicitly selected compatible profile voice retains network consent`() = runBlocking {
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

        val preparation = feature.prepare(
            TtsRequest(
                text = "Башня надежды",
                language = TtsLanguageSelection.Explicit(RUSSIAN),
            ),
        ).shouldBeInstanceOf<TtsPreparation.Ready>()

        preparation.request.voice shouldBe RUSSIAN_NETWORK
        preparation.request.networkProcessingAllowed shouldBe true
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
    ) : TtsEngine {
        override val catalogEntry = ENGINE
        override val presentation = PRESENTATION
        override val capabilities = CAPABILITIES
        var preparedRequest: ResolvedTtsRequest? = null

        override suspend fun inspectDevice() = TtsEngineDeviceAvailability.Available

        override suspend fun inspectVoices() = TtsVoiceInspection.Available(
            engine = ENGINE_ID,
            voices = voices,
            defaultVoice = defaultVoice,
        )

        override suspend fun prepare(request: ResolvedTtsRequest): TtsEnginePreparation.Ready {
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
        val RUSSIAN_NETWORK = voice("russian-network", RUSSIAN, TtsVoiceProcessing.NetworkRequired)
        val RUSSIAN_LOCAL = voice("russian-local", RUSSIAN, TtsVoiceProcessing.OnDevice)
        val READY_REQUEST = object : ReadyTtsEngineRequest {}

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
