package mihon.tts.ui.settings

import mihon.language.api.tag.LanguageTag
import mihon.tts.api.provider.TtsVoiceProcessing
import mihon.tts.api.voice.TtsVoice
import java.util.Locale

data class TtsLanguageOption(
    val tag: LanguageTag,
    val displayName: String,
)

fun ttsLanguageOptions(
    voices: List<TtsVoice>,
    excludedLanguages: Set<LanguageTag> = emptySet(),
    locale: Locale = Locale.getDefault(),
): List<TtsLanguageOption> = ttsLanguageOptions(
    voices = voices,
    includeVoice = { true },
    excludedLanguages = excludedLanguages,
    locale = locale,
)

fun ttsLanguageOptions(
    voices: List<TtsVoice>,
    allowNetworkVoices: Boolean,
    excludedLanguages: Set<LanguageTag> = emptySet(),
    locale: Locale = Locale.getDefault(),
): List<TtsLanguageOption> = ttsLanguageOptions(
    voices = voices,
    includeVoice = { allowNetworkVoices || it.processing != TtsVoiceProcessing.NetworkRequired },
    excludedLanguages = excludedLanguages,
    locale = locale,
)

private fun ttsLanguageOptions(
    voices: List<TtsVoice>,
    includeVoice: (TtsVoice) -> Boolean,
    excludedLanguages: Set<LanguageTag>,
    locale: Locale,
): List<TtsLanguageOption> {
    return voices
        .filter(includeVoice)
        .map(TtsVoice::language)
        .distinct()
        .filterNot(excludedLanguages::contains)
        .map { language -> TtsLanguageOption(language, language.displayName(locale)) }
        .sortedWith(compareBy({ it.displayName.lowercase(locale) }, { it.tag.value }))
}

fun List<TtsVoice>.compatibleWith(language: LanguageTag): List<TtsVoice> {
    val requested = Locale.forLanguageTag(language.value)
    return filter { voice ->
        val candidate = Locale.forLanguageTag(voice.language.value)
        voice.language == language || candidate.language == requested.language
    }.sortedWith(
        compareBy<TtsVoice>(
            { it.processing == TtsVoiceProcessing.NetworkRequired },
            { it.name.lowercase(Locale.ROOT) },
            { it.id.value },
        ),
    )
}

fun List<TtsVoice>.supports(
    language: LanguageTag,
    allowNetworkVoices: Boolean,
): Boolean = compatibleWith(language).any { voice ->
    allowNetworkVoices || voice.processing != TtsVoiceProcessing.NetworkRequired
}

fun LanguageTag.displayName(locale: Locale = Locale.getDefault()): String {
    return Locale.forLanguageTag(value).getDisplayName(locale).ifBlank { value }
}
