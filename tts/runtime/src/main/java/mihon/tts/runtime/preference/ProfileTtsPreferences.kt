package mihon.tts.runtime.preference

import mihon.language.api.tag.LanguageTag
import mihon.tts.api.engine.TtsEngineId
import mihon.tts.api.voice.TtsDefaultVoiceSelection
import mihon.tts.api.voice.TtsVoiceId
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.core.common.preference.ProfilePreferenceKeyPattern
import java.nio.charset.StandardCharsets
import java.util.Base64
import java.util.Locale

class ProfileTtsPreferences(
    private val preferenceStore: PreferenceStore,
    initialEngine: TtsEngineId?,
) {
    private val voices = mutableMapOf<LanguageTag, Preference<TtsVoiceId?>>()
    private val voiceOverrideLanguages = preferenceStore.getStringSet("tts_voice_override_languages", emptySet())
    val engine: Preference<TtsEngineId> = preferenceStore.getObjectFromString(
        key = "tts_engine",
        defaultValue = initialEngine ?: UNAVAILABLE_ENGINE,
        serializer = TtsEngineId::value,
        deserializer = ::TtsEngineId,
    )
    val speechRate: Preference<Float> = preferenceStore.getFloat("tts_speech_rate", 1f)
    val pitch: Preference<Float> = preferenceStore.getFloat("tts_pitch", 1f)
    val allowNetworkVoices: Preference<Boolean> = preferenceStore.getBoolean("tts_allow_network_voices", false)
    private val defaultVoice = preferenceStore.getObjectFromString(
        key = "tts_default_voice",
        defaultValue = null,
        serializer = ::serializeVoice,
        deserializer = ::deserializeVoice,
    )

    init {
        if (!engine.isSet() && initialEngine != null) {
            engine.set(initialEngine)
        }
    }

    fun voice(language: LanguageTag): Preference<TtsVoiceId?> {
        return synchronized(voices) {
            voices.getOrPut(language) {
                preferenceStore.getObjectFromString(
                    key = VOICE_KEY_FAMILY.key(language.value.lowercase(Locale.ROOT)),
                    defaultValue = null,
                    serializer = ::serializeVoice,
                    deserializer = ::deserializeVoice,
                )
            }
        }
    }

    fun voiceOverrides(): Map<LanguageTag, TtsVoiceId> {
        return voiceOverrideLanguages.get().mapNotNull { value ->
            val language = LanguageTag.parse(value) ?: return@mapNotNull null
            voice(language).get()?.let { language to it }
        }.toMap()
    }

    fun selectedDefaultVoice(): TtsDefaultVoiceSelection {
        return defaultVoice.get()
            ?.let(TtsDefaultVoiceSelection::Explicit)
            ?: TtsDefaultVoiceSelection.EngineDefault
    }

    fun setDefaultVoice(selection: TtsDefaultVoiceSelection) {
        when (selection) {
            TtsDefaultVoiceSelection.EngineDefault -> defaultVoice.delete()
            is TtsDefaultVoiceSelection.Explicit -> defaultVoice.set(selection.voice)
        }
    }

    fun setVoice(language: LanguageTag, voice: TtsVoiceId?) {
        val preference = voice(language)
        val key = language.value.lowercase(Locale.ROOT)
        val languages = voiceOverrideLanguages.get()
        if (voice == null) {
            preference.delete()
            voiceOverrideLanguages.set(languages - key)
        } else {
            preference.set(voice)
            voiceOverrideLanguages.set(languages + key)
        }
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
        private val UNAVAILABLE_ENGINE = TtsEngineId("tts-unavailable")
    }
}
