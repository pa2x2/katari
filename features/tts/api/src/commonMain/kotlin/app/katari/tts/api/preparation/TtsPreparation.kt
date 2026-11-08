package app.katari.tts.api.preparation

import app.katari.language.api.tag.LanguageTag
import app.katari.tts.api.engine.KnownTtsEngine
import app.katari.tts.api.engine.TtsEngineId
import app.katari.tts.api.provider.TtsProviderDisclosure
import app.katari.tts.api.provider.TtsProviderPresentation
import app.katari.tts.api.request.ResolvedTtsRequest
import app.katari.tts.api.voice.TtsVoice
import app.katari.tts.api.voice.TtsVoiceId

/** Opaque, process-local playback authority returned only by [app.katari.tts.api.TtsFeature.prepare]. */
interface ReadyTts

sealed interface TtsPreparation {
    data class Ready(
        val speech: ReadyTts,
        val request: ResolvedTtsRequest,
        val presentation: TtsProviderPresentation,
    ) : TtsPreparation

    data class LanguageChoiceRequired(
        val reason: TtsLanguageChoiceReason,
        val suggestedLanguages: List<LanguageTag> = emptyList(),
    ) : TtsPreparation

    data class EngineChoiceRequired(
        val reason: TtsEngineChoiceReason,
        val engines: List<KnownTtsEngine>,
    ) : TtsPreparation

    data class VoiceChoiceRequired(
        val engine: TtsEngineId,
        val language: LanguageTag,
        val reason: TtsVoiceChoiceReason,
        val voices: List<TtsVoice>,
    ) : TtsPreparation

    data class ProviderDisclosureRequired(
        val engine: TtsEngineId,
        val presentation: TtsProviderPresentation,
        val disclosure: TtsProviderDisclosure,
    ) : TtsPreparation

    data class SystemSetupRequired(
        val engine: TtsEngineId,
        val presentation: TtsProviderPresentation,
        val reason: TtsSystemSetupReason,
    ) : TtsPreparation

    data class Unavailable(
        val reason: TtsUnavailableReason,
    ) : TtsPreparation

    data class Rejected(
        val reason: TtsRejectionReason,
    ) : TtsPreparation
}

enum class TtsLanguageChoiceReason {
    Undetermined,
    Ambiguous,
}

sealed interface TtsEngineChoiceReason {
    data object NoEngineConfigured : TtsEngineChoiceReason

    data class SelectedEngineUnavailable(
        val engine: TtsEngineId,
    ) : TtsEngineChoiceReason
}

sealed interface TtsVoiceChoiceReason {
    data object NoCompatibleVoice : TtsVoiceChoiceReason

    data class SelectedVoiceUnavailable(
        val voice: TtsVoiceId,
    ) : TtsVoiceChoiceReason
}

sealed interface TtsSystemSetupReason {
    data object ServiceDisabled : TtsSystemSetupReason

    data object VoiceDataRequired : TtsSystemSetupReason

    data class ProviderActionRequired(
        val description: String,
    ) : TtsSystemSetupReason {
        init {
            require(description.isNotBlank())
        }
    }
}

sealed interface TtsUnavailableReason {
    data object ServiceMissing : TtsUnavailableReason

    data class UnsupportedLanguage(
        val language: LanguageTag,
    ) : TtsUnavailableReason

    data class NetworkVoiceProhibited(
        val voice: TtsVoiceId,
    ) : TtsUnavailableReason

    data class EngineUnavailable(
        val engine: TtsEngineId,
        val reason: String,
    ) : TtsUnavailableReason {
        init {
            require(reason.isNotBlank())
        }
    }

    data class ProviderInspectionFailed(
        val engine: TtsEngineId,
        val reason: String,
    ) : TtsUnavailableReason {
        init {
            require(reason.isNotBlank())
        }
    }
}

sealed interface TtsRejectionReason {
    data object BlankInput : TtsRejectionReason

    data class InputTooLarge(
        val actualCodePoints: Int,
        val maximumCodePoints: Int,
    ) : TtsRejectionReason {
        init {
            require(actualCodePoints > maximumCodePoints)
            require(maximumCodePoints > 0)
        }
    }

    data class UnsupportedSpeechRate(
        val value: Float,
    ) : TtsRejectionReason

    data class UnsupportedPitch(
        val value: Float,
    ) : TtsRejectionReason
}
