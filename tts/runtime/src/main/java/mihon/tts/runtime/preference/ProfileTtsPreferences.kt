package mihon.tts.runtime.preference

import mihon.language.api.tag.LanguageTag
import mihon.tts.api.engine.TtsEngineId
import mihon.tts.api.voice.TtsVoiceId
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.core.common.preference.ProfilePreferenceKeyPattern
import java.nio.charset.StandardCharsets
import java.util.Base64
import java.util.Locale

class ProfileTtsPreferences(
    private val preferenceStore: PreferenceStore,
    defaultEngine: TtsEngineId,
) {
    val engine: Preference<TtsEngineId> = preferenceStore.getObjectFromString(
        key = "tts_engine",
        defaultValue = defaultEngine,
        serializer = TtsEngineId::value,
        deserializer = ::TtsEngineId,
    )
    val speechRate: Preference<Float> = preferenceStore.getFloat("tts_speech_rate", 1f)
    val pitch: Preference<Float> = preferenceStore.getFloat("tts_pitch", 1f)
    val allowNetworkVoices: Preference<Boolean> = preferenceStore.getBoolean("tts_allow_network_voices", false)

    fun voice(language: LanguageTag): Preference<TtsVoiceId?> {
        return preferenceStore.getObjectFromString(
            key = VOICE_KEY_FAMILY.key(language.value.lowercase(Locale.ROOT)),
            defaultValue = null,
            serializer = ::serializeVoice,
            deserializer = ::deserializeVoice,
        )
    }

    private fun serializeVoice(voice: TtsVoiceId?): String {
        if (voice == null) return ""
        val opaqueVoice = Base64.getUrlEncoder().withoutPadding().encodeToString(
            voice.value.toByteArray(StandardCharsets.UTF_8),
        )
        return "${voice.provider.value}:${voice.engine.value}:$opaqueVoice"
    }

    private fun deserializeVoice(value: String): TtsVoiceId? {
        if (value.isEmpty()) return null
        val parts = value.split(':', limit = 3)
        if (parts.size != 3) return null
        return runCatching {
            TtsVoiceId(
                provider = mihon.tts.api.engine.TtsProviderId(parts[0]),
                engine = TtsEngineId(parts[1]),
                value = String(Base64.getUrlDecoder().decode(parts[2]), StandardCharsets.UTF_8),
            )
        }.getOrNull()
    }

    companion object {
        val VOICE_KEY_FAMILY = ProfilePreferenceKeyPattern.Prefix("tts_voice_")
    }
}
