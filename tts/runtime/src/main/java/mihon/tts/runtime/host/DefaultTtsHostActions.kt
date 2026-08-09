package mihon.tts.runtime.host

import kotlinx.coroutines.CancellationException
import mihon.language.api.tag.LanguageTag
import mihon.tts.api.availability.TtsDeviceAvailability
import mihon.tts.api.engine.KnownTtsEngine
import mihon.tts.api.engine.TtsEngineAction
import mihon.tts.api.engine.TtsEngineBuildAvailability
import mihon.tts.api.engine.TtsEngineId
import mihon.tts.api.engine.TtsEngineInspection
import mihon.tts.api.engine.TtsEngineState
import mihon.tts.api.engine.TtsEngineStatus
import mihon.tts.api.host.TtsHostActionResult
import mihon.tts.api.host.TtsHostActions
import mihon.tts.api.provider.TtsProviderDisclosure
import mihon.tts.api.voice.TtsDefaultVoiceSelection
import mihon.tts.api.voice.TtsVoiceId
import mihon.tts.api.voice.TtsVoiceInspection
import mihon.tts.runtime.preference.ProfileTtsPreferences
import mihon.tts.runtime.selection.ProfileTtsEngineResolver
import mihon.tts.spi.engine.KnownTtsEngineCatalog
import mihon.tts.spi.engine.TtsEngine
import mihon.tts.spi.engine.TtsEngineDeviceAvailability
import mihon.tts.spi.engine.TtsEngineRegistry
import mihon.tts.spi.setup.TtsEngineSetup
import mihon.tts.spi.setup.TtsEngineSetupRegistry
import mihon.tts.spi.setup.TtsSetupResult
import tachiyomi.core.common.preference.Preference

internal class DefaultTtsHostActions(
    private val preferences: ProfileTtsPreferences,
    private val engineRegistry: TtsEngineRegistry,
    catalog: KnownTtsEngineCatalog,
    private val setupRegistry: TtsEngineSetupRegistry,
    private val engineResolver: ProfileTtsEngineResolver,
) : TtsHostActions {
    override val knownEngines: List<KnownTtsEngine> = catalog.knownEngines
    override val selectedEngine: Preference<TtsEngineId> = preferences.engine
    override val speechRate: Preference<Float> = preferences.speechRate
    override val pitch: Preference<Float> = preferences.pitch
    override val allowNetworkVoices: Preference<Boolean> = preferences.allowNetworkVoices

    override suspend fun deviceAvailability(): TtsDeviceAvailability {
        if (knownEngines.isEmpty()) return TtsDeviceAvailability.NoEnginesInstalled
        val selected = engineResolver.resolve() ?: return TtsDeviceAvailability.EngineNotConfigured
        val engine = engineRegistry.find(selected)
            ?: return if (knownEngines.any { it.id == selected }) {
                TtsDeviceAvailability.SelectedEngineUnavailable(selected)
            } else {
                TtsDeviceAvailability.SelectedEngineMissing(selected)
            }
        return inspectDevice(engine).toApi(selected)
    }

    override suspend fun inspectEngines(): TtsEngineInspection {
        val resolvedEngine = engineResolver.resolve()
        val states = knownEngines.map { known ->
            val engine = engineRegistry.find(known.id)
            val buildAvailability = known.buildAvailability
            val status = when {
                buildAvailability is TtsEngineBuildAvailability.NotIncluded ->
                    TtsEngineStatus.Unavailable(buildAvailability.reason)
                engine == null -> TtsEngineStatus.NotInstalled
                else -> inspectDevice(engine).toStatus()
            }
            TtsEngineState(
                engine = known,
                presentation = engine?.presentation,
                status = status,
                action = status.action(),
                capabilities = engine?.capabilities,
            )
        }
        return TtsEngineInspection(
            engines = states,
            selectedEngine = resolvedEngine,
            selectionResolved = true,
        )
    }

    override suspend fun inspectVoices(engine: TtsEngineId): TtsVoiceInspection {
        val installed = engineRegistry.find(engine)
            ?: return TtsVoiceInspection.Unavailable(engine, "TTS engine is not available")
        return try {
            installed.refreshVoices()
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            TtsVoiceInspection.Failed(engine, "TTS voices could not be inspected")
        }
    }

    override suspend fun acknowledgeProviderDisclosure(
        engine: TtsEngineId,
        disclosure: TtsProviderDisclosure,
    ): TtsHostActionResult = performSetup(engine) {
        acknowledge(disclosure)
        TtsHostActionResult.Completed
    }

    override fun supportsSetup(engine: TtsEngineId): Boolean {
        return setupRegistry.findSetup(engine)?.supportsSetup == true
    }

    override suspend fun openSetup(engine: TtsEngineId): TtsHostActionResult = performSetup(engine) {
        openSetup().toApi()
    }

    override suspend fun installVoiceData(
        engine: TtsEngineId,
        languages: Set<LanguageTag>,
    ): TtsHostActionResult = performSetup(engine) {
        if (languages.isEmpty()) {
            TtsHostActionResult.Failed("At least one language is required")
        } else {
            installVoiceData(languages).toApi()
        }
    }

    override fun selectedVoice(language: LanguageTag): TtsVoiceId? = preferences.voice(language).get()

    override fun selectedDefaultVoice(): TtsDefaultVoiceSelection = preferences.selectedDefaultVoice()

    override fun selectedVoiceOverrides(): Map<LanguageTag, TtsVoiceId> = preferences.voiceOverrides()

    override fun setSelectedEngine(engine: TtsEngineId) {
        selectedEngine.set(engine)
    }

    override fun setSelectedVoice(language: LanguageTag, voice: TtsVoiceId?) {
        preferences.setVoice(language, voice)
    }

    override fun setSelectedDefaultVoice(voice: TtsDefaultVoiceSelection) {
        preferences.setDefaultVoice(voice)
    }

    private suspend fun inspectDevice(engine: TtsEngine): TtsEngineDeviceAvailability {
        return try {
            engine.inspectDevice()
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            TtsEngineDeviceAvailability.Failed("TTS engine availability could not be inspected")
        }
    }

    private suspend fun performSetup(
        engine: TtsEngineId,
        action: suspend TtsEngineSetup.() -> TtsHostActionResult,
    ): TtsHostActionResult {
        val setup = setupRegistry.findSetup(engine) ?: return TtsHostActionResult.SetupUnsupported
        return try {
            setup.action()
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            TtsHostActionResult.Failed("Unexpected TTS setup failure")
        }
    }

    private fun TtsEngineDeviceAvailability.toApi(engine: TtsEngineId): TtsDeviceAvailability {
        return when (this) {
            TtsEngineDeviceAvailability.Available -> TtsDeviceAvailability.Available
            TtsEngineDeviceAvailability.NotInstalled -> TtsDeviceAvailability.SelectedEngineMissing(engine)
            is TtsEngineDeviceAvailability.ConfigurationRequired ->
                TtsDeviceAvailability.SelectedEngineUnavailable(engine, reason)
            TtsEngineDeviceAvailability.ServiceMissing -> TtsDeviceAvailability.SelectedEngineUnavailable(
                engine,
                "TTS service is missing",
            )
            is TtsEngineDeviceAvailability.VoiceDataRequired ->
                TtsDeviceAvailability.SelectedEngineUnavailable(engine, reason ?: "Voice data is required")
            is TtsEngineDeviceAvailability.Unavailable -> TtsDeviceAvailability.SelectedEngineUnavailable(
                engine,
                reason,
            )
            is TtsEngineDeviceAvailability.Failed -> TtsDeviceAvailability.ProviderFailure(engine, reason)
        }
    }

    private fun TtsEngineDeviceAvailability.toStatus(): TtsEngineStatus {
        return when (this) {
            TtsEngineDeviceAvailability.Available -> TtsEngineStatus.Ready
            TtsEngineDeviceAvailability.NotInstalled -> TtsEngineStatus.NotInstalled
            is TtsEngineDeviceAvailability.ConfigurationRequired -> TtsEngineStatus.ConfigurationRequired(reason)
            TtsEngineDeviceAvailability.ServiceMissing -> TtsEngineStatus.Unavailable("TTS service is missing")
            is TtsEngineDeviceAvailability.VoiceDataRequired -> TtsEngineStatus.VoiceDataRequired(reason)
            is TtsEngineDeviceAvailability.Unavailable -> TtsEngineStatus.Unavailable(reason)
            is TtsEngineDeviceAvailability.Failed -> TtsEngineStatus.Failed(reason)
        }
    }

    private fun TtsEngineStatus.action(): TtsEngineAction? {
        return when (this) {
            TtsEngineStatus.NotInstalled -> TtsEngineAction.Install
            is TtsEngineStatus.ConfigurationRequired -> TtsEngineAction.Configure
            is TtsEngineStatus.VoiceDataRequired -> TtsEngineAction.SetupVoiceData
            else -> null
        }
    }

    private fun TtsSetupResult.toApi(): TtsHostActionResult {
        return when (this) {
            TtsSetupResult.Completed -> TtsHostActionResult.Completed
            is TtsSetupResult.Opened -> TtsHostActionResult.SetupOpened(destination)
            TtsSetupResult.Unsupported -> TtsHostActionResult.SetupUnsupported
            TtsSetupResult.ServiceMissing -> TtsHostActionResult.ServiceMissing
            TtsSetupResult.SettingsUnavailable -> TtsHostActionResult.SettingsUnavailable
            is TtsSetupResult.Failed -> TtsHostActionResult.Failed(reason)
        }
    }
}
