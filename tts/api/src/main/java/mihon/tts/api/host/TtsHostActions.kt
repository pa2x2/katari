package mihon.tts.api.host

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import mihon.language.api.tag.LanguageTag
import mihon.tts.api.availability.TtsDeviceAvailability
import mihon.tts.api.engine.KnownTtsEngine
import mihon.tts.api.engine.TtsEngineId
import mihon.tts.api.engine.TtsEngineInspection
import mihon.tts.api.provider.TtsProviderDisclosure
import mihon.tts.api.voice.TtsVoiceId
import mihon.tts.api.voice.TtsVoiceInspection
import tachiyomi.core.common.preference.Preference

/** Provider-neutral host boundary for settings and setup surfaces. */
interface TtsHostActions {
    val knownEngines: List<KnownTtsEngine>
    val selectedEngine: Preference<TtsEngineId>
    val speechRate: Preference<Float>
    val pitch: Preference<Float>
    val allowNetworkVoices: Preference<Boolean>

    suspend fun deviceAvailability(): TtsDeviceAvailability

    suspend fun inspectEngines(): TtsEngineInspection

    fun inspectEngineStates(): Flow<TtsEngineInspection> = flow {
        emit(inspectEngines())
    }

    suspend fun inspectVoices(engine: TtsEngineId): TtsVoiceInspection

    suspend fun acknowledgeProviderDisclosure(
        engine: TtsEngineId,
        disclosure: TtsProviderDisclosure,
    ): TtsHostActionResult

    fun supportsSetup(engine: TtsEngineId): Boolean

    suspend fun openSetup(engine: TtsEngineId): TtsHostActionResult

    suspend fun installVoiceData(
        engine: TtsEngineId,
        languages: Set<LanguageTag>,
    ): TtsHostActionResult

    fun selectedVoice(language: LanguageTag): TtsVoiceId?

    fun setSelectedEngine(engine: TtsEngineId)

    fun setSelectedVoice(language: LanguageTag, voice: TtsVoiceId?)
}

sealed interface TtsHostActionResult {
    data object Completed : TtsHostActionResult

    data class SetupOpened(
        val destination: TtsSetupDestination,
    ) : TtsHostActionResult

    data object SetupUnsupported : TtsHostActionResult

    data object ServiceMissing : TtsHostActionResult

    data object SettingsUnavailable : TtsHostActionResult

    data class Failed(
        val reason: String,
    ) : TtsHostActionResult {
        init {
            require(reason.isNotBlank())
        }
    }
}

enum class TtsSetupDestination {
    InApp,
    External,
}
