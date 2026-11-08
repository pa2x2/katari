package app.katari.entry.interactions.book.reader.translation

import app.katari.entry.interactions.translation.EntryTranslationLanguagesFeature
import app.katari.language.api.tag.LanguageTag
import app.katari.translation.ui.session.language.TranslationLanguageStore
import app.katari.translation.ui.session.language.TranslationStoredLanguages
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import tachiyomi.domain.entry.model.Entry

/**
 * Keeps the pinned language and target of the book the current chapter belongs to, so every reader session of that
 * book uses them. In a merged book that is the chapter's member, as downloads and background translations key them.
 */
internal class BookTranslationLanguageStore(
    private val feature: EntryTranslationLanguagesFeature,
    private val book: StateFlow<Entry?>,
) : TranslationLanguageStore {
    @OptIn(ExperimentalCoroutinesApi::class)
    override val languages: Flow<TranslationStoredLanguages> = book
        .filterNotNull()
        .distinctUntilChangedBy(Entry::id)
        .flatMapLatest(feature::observe)
        .map { choices ->
            TranslationStoredLanguages(source = choices.contentLanguage, target = choices.targetLanguage)
        }

    override suspend fun setSourceLanguage(language: LanguageTag?) {
        book.value?.let { feature.setContentLanguage(it, language) }
    }

    override suspend fun setTargetLanguage(language: LanguageTag?) {
        book.value?.let { feature.setTargetLanguage(it, language) }
    }
}
