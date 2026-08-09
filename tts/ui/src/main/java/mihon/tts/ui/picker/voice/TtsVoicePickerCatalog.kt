package mihon.tts.ui.picker.voice

import androidx.compose.runtime.Immutable
import mihon.language.api.tag.LanguageTag
import mihon.tts.api.provider.TtsVoiceProcessing
import mihon.tts.api.voice.TtsVoice
import mihon.tts.api.voice.TtsVoiceId
import mihon.tts.ui.settings.displayName
import java.util.Locale

@Immutable
internal data class TtsVoicePickerCatalog(
    val groups: List<TtsVoiceLanguageGroup>,
    private val entriesById: Map<TtsVoiceId, TtsVoicePickerEntry>,
) {
    fun find(voice: TtsVoiceId?): TtsVoicePickerEntry? = voice?.let(entriesById::get)

    fun search(query: String, locale: Locale): List<TtsVoiceLanguageGroup> {
        if (query.isBlank()) return groups
        val normalizedQuery = query.lowercase(locale)
        return groups.mapNotNull { group ->
            group.copy(voices = group.voices.filter { it.matches(normalizedQuery) })
                .takeIf { it.voices.isNotEmpty() }
        }
    }
}

@Immutable
internal data class TtsVoiceLanguageGroup(
    val language: LanguageTag,
    val displayName: String,
    val voices: List<TtsVoicePickerEntry>,
)

@Immutable
internal data class TtsVoicePickerEntry(
    val voice: TtsVoice,
    val languageDisplayName: String,
    private val searchText: List<String>,
) {
    fun matches(normalizedQuery: String): Boolean = searchText.any { normalizedQuery in it }
}

internal fun ttsVoicePickerCatalog(
    voices: List<TtsVoice>,
    language: LanguageTag?,
    locale: Locale,
): TtsVoicePickerCatalog {
    val requestedLanguage = language?.let { Locale.forLanguageTag(it.value).language }
    val entries = voices.mapNotNull { voice ->
        if (requestedLanguage != null) {
            val candidateLanguage = Locale.forLanguageTag(voice.language.value).language
            if (voice.language != language && candidateLanguage != requestedLanguage) return@mapNotNull null
        }
        val languageDisplayName = voice.language.displayName(locale)
        TtsVoicePickerEntry(
            voice = voice,
            languageDisplayName = languageDisplayName,
            searchText = listOf(
                voice.name.lowercase(locale),
                voice.id.value.lowercase(locale),
                voice.language.value.lowercase(locale),
                languageDisplayName.lowercase(locale),
            ),
        )
    }
    val groups = entries
        .groupBy { it.voice.language }
        .map { (tag, groupedVoices) ->
            TtsVoiceLanguageGroup(
                language = tag,
                displayName = groupedVoices.first().languageDisplayName,
                voices = groupedVoices.sortedWith(
                    compareBy<TtsVoicePickerEntry>(
                        { it.voice.processing == TtsVoiceProcessing.NetworkRequired },
                        { it.voice.name.lowercase(locale) },
                        { it.voice.id.value },
                    ),
                ),
            )
        }
        .sortedWith(compareBy({ it.displayName.lowercase(locale) }, { it.language.value }))
    return TtsVoicePickerCatalog(
        groups = groups,
        entriesById = entries.associateBy { it.voice.id },
    )
}
