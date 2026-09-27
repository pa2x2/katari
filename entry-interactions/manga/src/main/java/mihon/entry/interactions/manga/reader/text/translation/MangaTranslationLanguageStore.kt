package mihon.entry.interactions.manga.reader.text.translation

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import mihon.entry.interactions.translation.EntryTranslationLanguagesFeature
import mihon.language.api.tag.LanguageTag
import mihon.translation.ui.session.language.TranslationLanguageStore
import mihon.translation.ui.session.language.TranslationStoredLanguages
import tachiyomi.domain.entry.model.Entry

/**
 * Keeps the series' page language and translation target, so every reader session of the series uses them. The
 * reader loads its series after it starts, so choices made before then are not kept.
 */
internal class MangaTranslationLanguageStore(
    private val feature: EntryTranslationLanguagesFeature,
    private val series: StateFlow<Entry?>,
) : TranslationLanguageStore {
    @OptIn(ExperimentalCoroutinesApi::class)
    override val languages: Flow<TranslationStoredLanguages> = series
        .filterNotNull()
        .distinctUntilChangedBy(Entry::id)
        .flatMapLatest(feature::observe)
        .map { choices ->
            TranslationStoredLanguages(source = choices.contentLanguage, target = choices.targetLanguage)
        }

    override suspend fun setSourceLanguage(language: LanguageTag?) {
        series.value?.let { feature.setContentLanguage(it, language) }
    }

    override suspend fun setTargetLanguage(language: LanguageTag?) {
        series.value?.let { feature.setTargetLanguage(it, language) }
    }
}
