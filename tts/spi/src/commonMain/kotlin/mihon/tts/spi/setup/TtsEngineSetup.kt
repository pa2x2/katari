package mihon.tts.spi.setup

import mihon.language.api.tag.LanguageTag
import mihon.tts.api.engine.TtsEngineId
import mihon.tts.api.host.TtsSetupDestination
import mihon.tts.api.provider.TtsProviderDisclosure

interface TtsEngineSetup {
    val engine: TtsEngineId
    val supportsSetup: Boolean
        get() = false

    suspend fun acknowledge(disclosure: TtsProviderDisclosure)

    suspend fun openSetup(): TtsSetupResult

    suspend fun installVoiceData(languages: Set<LanguageTag>): TtsSetupResult
}

interface TtsEngineSetupRegistry {
    fun findSetup(engine: TtsEngineId): TtsEngineSetup?
}

sealed interface TtsSetupResult {
    data class Opened(
        val destination: TtsSetupDestination,
    ) : TtsSetupResult

    data object Completed : TtsSetupResult

    data object Unsupported : TtsSetupResult

    data object ServiceMissing : TtsSetupResult

    data object SettingsUnavailable : TtsSetupResult

    data class Failed(
        val reason: String,
    ) : TtsSetupResult {
        init {
            require(reason.isNotBlank())
        }
    }
}
