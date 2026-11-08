package app.katari.tts.runtime.feature

import app.katari.language.api.tag.LanguageTag
import app.katari.tts.api.engine.KnownTtsEngine
import app.katari.tts.api.engine.TtsEngineArtwork
import app.katari.tts.api.engine.TtsEngineBuildAvailability
import app.katari.tts.api.engine.TtsEngineDetails
import app.katari.tts.api.engine.TtsEngineId
import app.katari.tts.api.engine.TtsProviderId
import app.katari.tts.api.preparation.TtsPreparation
import app.katari.tts.api.preparation.TtsUnavailableReason
import app.katari.tts.api.preparation.TtsVoiceChoiceReason
import app.katari.tts.api.provider.TtsInputLimit
import app.katari.tts.api.provider.TtsOptionalCapability
import app.katari.tts.api.provider.TtsParameterSupport
import app.katari.tts.api.provider.TtsProviderCapabilities
import app.katari.tts.api.provider.TtsProviderPresentation
import app.katari.tts.api.provider.TtsVoiceProcessing
import app.katari.tts.api.request.ResolvedTtsRequest
import app.katari.tts.api.request.TtsLanguageSelection
import app.katari.tts.api.request.TtsRequest
import app.katari.tts.api.voice.TtsDefaultVoiceSelection
import app.katari.tts.api.voice.TtsVoice
import app.katari.tts.api.voice.TtsVoiceId
import app.katari.tts.api.voice.TtsVoiceInspection
import app.katari.tts.runtime.audio.TtsAudioFocus
import app.katari.tts.runtime.preference.ProfileTtsPreferences
import app.katari.tts.spi.engine.KnownTtsEngineCatalog
import app.katari.tts.spi.engine.ReadyTtsEngineRequest
import app.katari.tts.spi.engine.TtsEngine
import app.katari.tts.spi.engine.TtsEngineDeviceAvailability
import app.katari.tts.spi.engine.TtsEngineExecution
import app.katari.tts.spi.engine.TtsEnginePreparation
import app.katari.tts.spi.engine.TtsEngineRegistry
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.InMemoryPreferenceStore

class DefaultTtsFeatureVoiceResolutionTest {

    @Test
    fun `a network voice is used only with the profile's opt-in`() = runBlocking<Unit> {
        val russian = TtsRequest(text = "Башня надежды", language = TtsLanguageSelection.Explicit(RUSSIAN))
        val automatic = feature(
            engine = FakeEngine(voices = listOf(RUSSIAN_NETWORK), defaultVoice = RUSSIAN_NETWORK.id),
            preferences = ProfileTtsPreferences(InMemoryPreferenceStore(), ENGINE_ID),
        )
        val explicitPreferences = ProfileTtsPreferences(InMemoryPreferenceStore(), ENGINE_ID).apply {
            setDefaultVoice(TtsDefaultVoiceSelection.Explicit(RUSSIAN_NETWORK.id))
        }
        val explicit = feature(
            engine = FakeEngine(voices = listOf(RUSSIAN_NETWORK, RUSSIAN_LOCAL), defaultVoice = RUSSIAN_LOCAL.id),
            preferences = explicitPreferences,
        )
        val prohibited = TtsPreparation.Unavailable(TtsUnavailableReason.NetworkVoiceProhibited(RUSSIAN_NETWORK.id))

        automatic.prepare(russian) shouldBe prohibited
        explicit.prepare(russian) shouldBe prohibited

        explicitPreferences.allowNetworkVoices.set(true)
        val permitted = explicit.prepare(russian).shouldBeInstanceOf<TtsPreparation.Ready>()
        permitted.request.voice shouldBe RUSSIAN_NETWORK
        permitted.request.networkProcessingAllowed shouldBe true
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
            override val isOffered = true

            override fun request(onLost: () -> Unit) = true

            override fun abandon() = Unit
        },
    )

    private class FakeEngine(
        private val voices: List<TtsVoice>,
        private val defaultVoice: TtsVoiceId?,
        private val prepareFailure: RuntimeException? = null,
    ) : TtsEngine {
        override val catalogEntry = ENGINE
        override val presentation = PRESENTATION
        override val capabilities = CAPABILITIES

        override suspend fun inspectDevice() = TtsEngineDeviceAvailability.Available

        override suspend fun inspectVoices() = TtsVoiceInspection.Available(
            engine = ENGINE_ID,
            voices = voices,
            defaultVoice = defaultVoice,
        )

        override suspend fun prepare(request: ResolvedTtsRequest): TtsEnginePreparation.Ready {
            prepareFailure?.let { throw it }
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
        val ENGLISH_LOCAL = voice("english-local", ENGLISH, TtsVoiceProcessing.OnDevice)
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
