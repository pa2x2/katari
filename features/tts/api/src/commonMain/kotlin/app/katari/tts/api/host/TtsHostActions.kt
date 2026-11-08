package app.katari.tts.api.host

import app.katari.language.api.tag.LanguageTag
import app.katari.tts.api.availability.TtsDeviceAvailability
import app.katari.tts.api.engine.KnownTtsEngine
import app.katari.tts.api.engine.TtsEngineId
import app.katari.tts.api.engine.TtsEngineInspection
import app.katari.tts.api.provider.TtsProviderDisclosure
import app.katari.tts.api.voice.TtsDefaultVoiceSelection
import app.katari.tts.api.voice.TtsVoiceId
import app.katari.tts.api.voice.TtsVoiceInspection
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import tachiyomi.core.common.preference.Preference

/** Provider-neutral host boundary for settings and setup surfaces. */
interface TtsHostActions {
    /**
     * False where the build carries no speech provider, so the speech settings and every Listen action are hidden. A
     * device that only has no engine installed right now still offers speech, with [knownEngines] empty.
     */
    val isOffered: Boolean
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

    fun selectedDefaultVoice(): TtsDefaultVoiceSelection = TtsDefaultVoiceSelection.EngineDefault

    fun selectedVoiceOverrides(): Map<LanguageTag, TtsVoiceId>

    fun setSelectedEngine(engine: TtsEngineId)

    fun setSelectedVoice(language: LanguageTag, voice: TtsVoiceId?)

    fun setSelectedDefaultVoice(voice: TtsDefaultVoiceSelection) = Unit
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
