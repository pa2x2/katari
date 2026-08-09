package mihon.tts.api.voice

import mihon.language.api.tag.LanguageTag
import mihon.tts.api.engine.TtsEngineId
import mihon.tts.api.engine.TtsProviderId
import mihon.tts.api.provider.TtsVoiceProcessing

data class TtsVoiceId(
    val provider: TtsProviderId,
    val engine: TtsEngineId,
    val value: String,
) {
    init {
        require(value.isNotBlank())
    }
}

data class TtsVoice(
    val id: TtsVoiceId,
    val name: String,
    val language: LanguageTag,
    val processing: TtsVoiceProcessing,
    val quality: TtsVoiceQuality = TtsVoiceQuality.Unspecified,
    val latency: TtsVoiceLatency = TtsVoiceLatency.Unspecified,
) {
    init {
        require(name.isNotBlank())
    }
}

enum class TtsVoiceQuality {
    VeryLow,
    Low,
    Normal,
    High,
    VeryHigh,
    Unspecified,
}

enum class TtsVoiceLatency {
    VeryLow,
    Low,
    Normal,
    High,
    VeryHigh,
    Unspecified,
}

sealed interface TtsVoiceSelection {
    data object LanguageDefault : TtsVoiceSelection

    data class Explicit(
        val voice: TtsVoiceId,
    ) : TtsVoiceSelection
}
