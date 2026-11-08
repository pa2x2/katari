package app.katari.tts.api.request

import app.katari.language.api.identification.TextLanguageResolutionContext
import app.katari.language.api.tag.LanguageTag
import app.katari.tts.api.engine.TtsEngineId
import app.katari.tts.api.engine.TtsEngineSelection
import app.katari.tts.api.voice.TtsVoice
import app.katari.tts.api.voice.TtsVoiceSelection

data class TtsRequest(
    val text: String,
    val language: TtsLanguageSelection = TtsLanguageSelection.Automatic,
    val engine: TtsEngineSelection = TtsEngineSelection.ProfileDefault,
    val voice: TtsVoiceSelection = TtsVoiceSelection.LanguageDefault,
    val parameters: TtsParameterSelection = TtsParameterSelection.ProfileDefault,
    val processingPolicy: TtsProcessingPolicy = TtsProcessingPolicy.ProfileDefault,
    val languageContext: TextLanguageResolutionContext = TextLanguageResolutionContext(),
)

sealed interface TtsLanguageSelection {
    data object Automatic : TtsLanguageSelection

    data class Explicit(
        val language: LanguageTag,
    ) : TtsLanguageSelection
}

data class TtsParameters(
    val speechRate: Float = 1f,
    val pitch: Float = 1f,
) {
    init {
        require(speechRate > 0f)
        require(pitch > 0f)
    }
}

sealed interface TtsParameterSelection {
    data object ProfileDefault : TtsParameterSelection

    data class Explicit(
        val parameters: TtsParameters,
    ) : TtsParameterSelection
}

enum class TtsProcessingPolicy {
    ProfileDefault,
    OnDeviceOnly,
    NetworkAllowed,
}

data class ResolvedTtsRequest(
    val text: String,
    val language: LanguageTag,
    val engine: TtsEngineId,
    val voice: TtsVoice,
    val parameters: TtsParameters,
    val networkProcessingAllowed: Boolean,
)
