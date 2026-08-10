package mihon.tts.api.engine

import mihon.tts.api.provider.TtsProviderCapabilities
import mihon.tts.api.provider.TtsProviderDisclosure
import mihon.tts.api.provider.TtsProviderPresentation

data class TtsEngineInspection(
    val engines: List<TtsEngineState>,
    val selectedEngine: TtsEngineId?,
    val selectionResolved: Boolean = true,
)

data class TtsEngineState(
    val engine: KnownTtsEngine,
    val presentation: TtsProviderPresentation?,
    val status: TtsEngineStatus,
    val action: TtsEngineAction? = null,
    val capabilities: TtsProviderCapabilities? = null,
)

sealed interface TtsEngineStatus {
    data object Checking : TtsEngineStatus

    data object Ready : TtsEngineStatus

    data object NotInstalled : TtsEngineStatus

    data class ConfigurationRequired(
        val reason: String,
    ) : TtsEngineStatus {
        init {
            require(reason.isNotBlank())
        }
    }

    data class ProviderDisclosureRequired(
        val disclosure: TtsProviderDisclosure,
    ) : TtsEngineStatus

    data class VoiceDataRequired(
        val reason: String? = null,
    ) : TtsEngineStatus {
        init {
            require(reason == null || reason.isNotBlank())
        }
    }

    data class Unavailable(
        val reason: String? = null,
    ) : TtsEngineStatus {
        init {
            require(reason == null || reason.isNotBlank())
        }
    }

    data class Failed(
        val reason: String,
    ) : TtsEngineStatus {
        init {
            require(reason.isNotBlank())
        }
    }
}

enum class TtsEngineAction {
    Install,
    Configure,
    SetupVoiceData,
}
