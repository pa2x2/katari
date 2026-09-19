package mihon.translation.runtime.preference

import mihon.language.api.tag.LanguageTag
import mihon.translation.api.engine.TranslationEngineId
import mihon.translation.api.request.TranslationTargetLanguageSelection
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore

class ProfileTranslationPreferences(
    preferenceStore: PreferenceStore,
    private val defaultEngine: TranslationEngineId,
) {
    val engine: Preference<TranslationEngineId> = preferenceStore.getObjectFromString(
        key = "translation_engine",
        defaultValue = defaultEngine,
        serializer = TranslationEngineId::value,
        deserializer = ::TranslationEngineId,
    )

    val targetLanguage: Preference<TranslationTargetLanguageSelection> = preferenceStore.getObjectFromString(
        key = "translation_target_language",
        defaultValue = TranslationTargetLanguageSelection.Default,
        serializer = ::serializeTargetLanguage,
        deserializer = ::deserializeTargetLanguage,
    )

    val recentLanguages: Preference<List<LanguageTag>> = preferenceStore.getObjectFromString(
        key = "translation_recent_languages",
        defaultValue = emptyList(),
        serializer = { languages -> languages.joinToString(RECENT_LANGUAGE_SEPARATOR) { it.value } },
        deserializer = { raw ->
            raw.split(RECENT_LANGUAGE_SEPARATOR)
                .mapNotNull(LanguageTag::parse)
                .distinct()
        },
    )

    private fun serializeTargetLanguage(selection: TranslationTargetLanguageSelection): String {
        return when (selection) {
            TranslationTargetLanguageSelection.Default -> DEFAULT_TARGET_VALUE
            is TranslationTargetLanguageSelection.Explicit -> selection.language.value
        }
    }

    private fun deserializeTargetLanguage(value: String): TranslationTargetLanguageSelection {
        if (value == DEFAULT_TARGET_VALUE) return TranslationTargetLanguageSelection.Default
        return LanguageTag.parse(value)
            ?.let(TranslationTargetLanguageSelection::Explicit)
            ?: TranslationTargetLanguageSelection.Default
    }

    private companion object {
        const val DEFAULT_TARGET_VALUE = "default"
        const val RECENT_LANGUAGE_SEPARATOR = ","
    }
}

/**
 * Moves [language] to the front of the recently used languages, dropping its previous occurrence
 * and capping the list at [limit] entries, most recently used first.
 */
fun List<LanguageTag>.withRecentUse(
    language: LanguageTag,
    limit: Int = DEFAULT_RECENT_LANGUAGES_LIMIT,
): List<LanguageTag> = (listOf(language) + filterNot { it == language }).take(limit)

const val DEFAULT_RECENT_LANGUAGES_LIMIT = 6
