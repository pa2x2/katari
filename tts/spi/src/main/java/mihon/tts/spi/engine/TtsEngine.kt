package mihon.tts.spi.engine

import mihon.tts.api.engine.KnownTtsEngine
import mihon.tts.api.engine.TtsEngineId
import mihon.tts.api.preparation.TtsSystemSetupReason
import mihon.tts.api.preparation.TtsUnavailableReason
import mihon.tts.api.provider.TtsProviderCapabilities
import mihon.tts.api.provider.TtsProviderDisclosure
import mihon.tts.api.provider.TtsProviderPresentation
import mihon.tts.api.request.ResolvedTtsRequest
import mihon.tts.api.voice.TtsVoiceInspection

/** Provider adapter for bounded foreground speech. */
interface TtsEngine {
    val catalogEntry: KnownTtsEngine
    val presentation: TtsProviderPresentation
    val capabilities: TtsProviderCapabilities

    /** Request-independent inspection. Implementations must not send or manufacture user text. */
    suspend fun inspectDevice(): TtsEngineDeviceAvailability

    /** Voice catalog inspection. Implementations must not send or manufacture user text. */
    suspend fun inspectVoices(): TtsVoiceInspection

    suspend fun prepare(request: ResolvedTtsRequest): TtsEnginePreparation

    suspend fun revalidate(ready: ReadyTtsEngineRequest): TtsEnginePreparation

    suspend fun play(ready: ReadyTtsEngineRequest): TtsEngineExecution
}

sealed interface TtsEngineDeviceAvailability {
    data object Available : TtsEngineDeviceAvailability

    data object NotInstalled : TtsEngineDeviceAvailability

    data class ConfigurationRequired(
        val reason: String,
    ) : TtsEngineDeviceAvailability {
        init {
            require(reason.isNotBlank())
        }
    }

    data object ServiceMissing : TtsEngineDeviceAvailability

    data class VoiceDataRequired(
        val reason: String? = null,
    ) : TtsEngineDeviceAvailability {
        init {
            require(reason == null || reason.isNotBlank())
        }
    }

    data class Unavailable(
        val reason: String? = null,
    ) : TtsEngineDeviceAvailability {
        init {
            require(reason == null || reason.isNotBlank())
        }
    }

    data class Failed(
        val reason: String,
    ) : TtsEngineDeviceAvailability {
        init {
            require(reason.isNotBlank())
        }
    }
}

/** Provider-owned opaque state. Only the engine that produced it may execute it. */
interface ReadyTtsEngineRequest

sealed interface TtsEnginePreparation {
    data class Ready(
        val request: ReadyTtsEngineRequest,
    ) : TtsEnginePreparation

    data class ProviderDisclosureRequired(
        val disclosure: TtsProviderDisclosure,
    ) : TtsEnginePreparation

    data class SystemSetupRequired(
        val reason: TtsSystemSetupReason,
    ) : TtsEnginePreparation

    data class Unavailable(
        val reason: TtsUnavailableReason,
    ) : TtsEnginePreparation
}

interface TtsEngineRegistry {
    val engines: List<TtsEngine>

    fun find(engine: TtsEngineId): TtsEngine?
}

interface KnownTtsEngineCatalog {
    val knownEngines: List<KnownTtsEngine>
}
