package app.katari.translation.runtime.preference

import app.katari.language.api.tag.LanguageTag
import app.katari.translation.api.engine.TranslationEngineId
import app.katari.translation.api.preference.getLanguageTags
import app.katari.translation.api.preference.getTargetLanguageSelection
import app.katari.translation.api.request.TranslationTargetLanguageSelection
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

    val targetLanguage: Preference<TranslationTargetLanguageSelection> =
        preferenceStore.getTargetLanguageSelection("translation_target_language")

    val recentLanguages: Preference<List<LanguageTag>> = preferenceStore.getLanguageTags("translation_recent_languages")
}

/**
 * Moves [language] to the front of the recently used languages, dropping its previous occurrence
 * and capping the list at [limit] entries, most recently used first.
 */
internal fun List<LanguageTag>.withRecentUse(
    language: LanguageTag,
    limit: Int = DEFAULT_RECENT_LANGUAGES_LIMIT,
): List<LanguageTag> = (listOf(language) + filterNot { it == language }).take(limit)

internal const val DEFAULT_RECENT_LANGUAGES_LIMIT = 6
