package mihon.tts.provider.android.voice

import android.speech.tts.Voice
import mihon.language.api.tag.LanguageTag
import mihon.tts.api.engine.TtsEngineId
import mihon.tts.api.engine.TtsProviderId
import mihon.tts.api.provider.TtsVoiceProcessing
import mihon.tts.api.voice.TtsVoice
import mihon.tts.api.voice.TtsVoiceId
import mihon.tts.api.voice.TtsVoiceLatency
import mihon.tts.api.voice.TtsVoiceQuality

internal fun Voice.toApiVoice(
    provider: TtsProviderId,
    engine: TtsEngineId,
): TtsVoice? {
    val language = LanguageTag.parse(locale.toLanguageTag()) ?: return null
    return TtsVoice(
        id = TtsVoiceId(provider, engine, name),
        name = name,
        language = language,
        processing = when {
            isNetworkConnectionRequired -> TtsVoiceProcessing.NetworkRequired
            else -> TtsVoiceProcessing.OnDevice
        },
        quality = quality.toVoiceQuality(),
        latency = latency.toVoiceLatency(),
    )
}

private fun Int.toVoiceQuality(): TtsVoiceQuality = when (this) {
    Voice.QUALITY_VERY_LOW -> TtsVoiceQuality.VeryLow
    Voice.QUALITY_LOW -> TtsVoiceQuality.Low
    Voice.QUALITY_NORMAL -> TtsVoiceQuality.Normal
    Voice.QUALITY_HIGH -> TtsVoiceQuality.High
    Voice.QUALITY_VERY_HIGH -> TtsVoiceQuality.VeryHigh
    else -> TtsVoiceQuality.Unspecified
}

private fun Int.toVoiceLatency(): TtsVoiceLatency = when (this) {
    Voice.LATENCY_VERY_LOW -> TtsVoiceLatency.VeryLow
    Voice.LATENCY_LOW -> TtsVoiceLatency.Low
    Voice.LATENCY_NORMAL -> TtsVoiceLatency.Normal
    Voice.LATENCY_HIGH -> TtsVoiceLatency.High
    Voice.LATENCY_VERY_HIGH -> TtsVoiceLatency.VeryHigh
    else -> TtsVoiceLatency.Unspecified
}
