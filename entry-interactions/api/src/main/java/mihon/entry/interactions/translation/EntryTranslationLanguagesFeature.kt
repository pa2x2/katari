package mihon.entry.interactions.translation

import eu.kanade.tachiyomi.source.entry.EntryType
import kotlinx.coroutines.flow.Flow
import mihon.language.api.tag.LanguageTag
import tachiyomi.domain.entry.model.Entry

/** Translation languages chosen for one series, used instead of the defaults while it is read. */
data class EntryTranslationLanguageChoices(
    /** The language the series' text is written in; null follows the language its source declares. */
    val contentLanguage: LanguageTag? = null,
    /** The language the series is translated into; null follows the profile's target. */
    val targetLanguage: LanguageTag? = null,
    /** Whether every download of the series is also translated in the background. */
    val translateDownloads: Boolean = false,
)

/** Feature-owned boundary for the translation languages a reader chooses per series. */
interface EntryTranslationLanguagesFeature {
    fun isApplicable(type: EntryType): Boolean

    /** The languages chosen for [entry]; none for types the feature does not apply to. */
    fun observe(entry: Entry): Flow<EntryTranslationLanguageChoices>

    /** Pins the series' content language, or follows its source again when [language] is null. */
    suspend fun setContentLanguage(entry: Entry, language: LanguageTag?): EntryTranslationLanguagesWriteResult

    /** Pins the series' target language, or follows the profile again when [language] is null. */
    suspend fun setTargetLanguage(entry: Entry, language: LanguageTag?): EntryTranslationLanguagesWriteResult

    /** Sets whether every download of the series is also translated in the background. */
    suspend fun setTranslateDownloads(entry: Entry, enabled: Boolean): EntryTranslationLanguagesWriteResult
}

sealed interface EntryTranslationLanguagesWriteResult {
    data object Applied : EntryTranslationLanguagesWriteResult

    data class Inapplicable(val type: EntryType) : EntryTranslationLanguagesWriteResult
}
