package mihon.entry.interactions.book.reader.translation

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import mihon.entry.interactions.translation.EntryTranslationLanguagesFeature
import mihon.language.api.tag.LanguageTag
import mihon.translation.ui.session.language.TranslationLanguageStore
import mihon.translation.ui.session.language.TranslationStoredLanguages
import tachiyomi.domain.entry.model.Entry

/** Keeps the book's pinned language and its target, so every reader session of the book uses them. */
internal class BookTranslationLanguageStore(
    private val feature: EntryTranslationLanguagesFeature,
    private val entry: Entry,
) : TranslationLanguageStore {
    override val languages: Flow<TranslationStoredLanguages> = feature.observe(entry)
        .map { choices ->
            TranslationStoredLanguages(source = choices.contentLanguage, target = choices.targetLanguage)
        }

    override suspend fun setSourceLanguage(language: LanguageTag?) {
        feature.setContentLanguage(entry, language)
    }

    override suspend fun setTargetLanguage(language: LanguageTag?) {
        feature.setTargetLanguage(entry, language)
    }
}
