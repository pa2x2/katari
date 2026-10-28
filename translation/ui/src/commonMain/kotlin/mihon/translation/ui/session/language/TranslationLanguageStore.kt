package mihon.translation.ui.session.language

import kotlinx.coroutines.flow.Flow
import mihon.language.api.tag.LanguageTag

/**
 * Keeps the languages chosen for the content being read, such as one series, beyond a single reader session.
 * A null language follows its default: detection for the source, the profile's target for the target.
 */
interface TranslationLanguageStore {
    val languages: Flow<TranslationStoredLanguages>

    suspend fun setSourceLanguage(language: LanguageTag?)

    suspend fun setTargetLanguage(language: LanguageTag?)
}

data class TranslationStoredLanguages(
    val source: LanguageTag? = null,
    val target: LanguageTag? = null,
)
