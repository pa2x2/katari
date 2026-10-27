package mihon.desktop.translation

import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.source.entry.EntryType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import mihon.entry.interactions.translation.EntryTranslationLanguageChoices
import mihon.entry.interactions.translation.EntryTranslationLanguagesFeature
import mihon.entry.interactions.translation.EntryTranslationLanguagesWriteResult
import mihon.language.api.tag.LanguageTag
import tachiyomi.domain.entry.model.Entry

/**
 * Per-series translation languages as a feature that applies to no entry type: every series follows the defaults and
 * nothing is stored. Stands in for the shared feature, which desktop can host once it has entry types that translate
 * and the application services their features contribute to.
 */
@Inject
class NoEntryTranslationLanguagesFeature : EntryTranslationLanguagesFeature {
    override fun isApplicable(type: EntryType): Boolean = false

    override fun observe(entry: Entry): Flow<EntryTranslationLanguageChoices> =
        flowOf(EntryTranslationLanguageChoices())

    override suspend fun setContentLanguage(
        entry: Entry,
        language: LanguageTag?,
    ): EntryTranslationLanguagesWriteResult = EntryTranslationLanguagesWriteResult.Inapplicable(entry.type)

    override suspend fun setTargetLanguage(entry: Entry, language: LanguageTag?): EntryTranslationLanguagesWriteResult =
        EntryTranslationLanguagesWriteResult.Inapplicable(entry.type)

    override suspend fun setTranslateDownloads(entry: Entry, enabled: Boolean): EntryTranslationLanguagesWriteResult =
        EntryTranslationLanguagesWriteResult.Inapplicable(entry.type)
}
