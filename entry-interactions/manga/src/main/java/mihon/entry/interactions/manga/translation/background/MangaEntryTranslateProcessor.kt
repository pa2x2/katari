package mihon.entry.interactions.manga.translation.background

import eu.kanade.tachiyomi.source.entry.EntryCatalogueSource
import eu.kanade.tachiyomi.source.entry.EntryType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import mihon.entry.interactions.manga.reader.text.session.declaredContentLanguage
import mihon.entry.interactions.manga.translation.artifact.MangaChapterTranslationSetup
import mihon.entry.interactions.manga.translation.artifact.MangaChapterTranslationStore
import mihon.entry.interactions.manga.translation.context.mangaWorkContext
import mihon.entry.interactions.manga.translation.pages.MangaDownloadedChapterPages
import mihon.entry.interactions.translate.EntryTranslateFailure
import mihon.entry.interactions.translate.EntryTranslatePreparation
import mihon.entry.interactions.translate.EntryTranslateProcessor
import mihon.entry.interactions.translate.EntryTranslateProgress
import mihon.entry.interactions.translate.EntryTranslateResult
import mihon.entry.interactions.translate.EntryTranslateSetup
import mihon.entry.interactions.translation.EntryTranslationLanguageChoices
import mihon.translation.api.request.ResolvedTranslationRoute
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.source.service.SourceManager

/** Translates downloaded manga chapters into stored translations the reader opens with. */
internal class MangaEntryTranslateProcessor(
    private val sourceManager: SourceManager,
    private val store: () -> MangaChapterTranslationStore,
    private val pages: () -> MangaDownloadedChapterPages,
    private val translator: () -> MangaChapterTranslator,
    private val preparer: () -> MangaChapterTranslationPreparer,
) : EntryTranslateProcessor {
    override val type = EntryType.MANGA

    override val changes: Flow<Unit> = flow { emitAll(store().changes) }

    override suspend fun prepare(
        entry: Entry,
        languages: EntryTranslationLanguageChoices,
    ): EntryTranslatePreparation {
        val source = sourceManager.get(entry.source) as? EntryCatalogueSource
        return preparer().prepare(
            pageLanguage = languages.contentLanguage ?: declaredContentLanguage(source?.lang),
            targetLanguage = languages.targetLanguage,
        )
    }

    override suspend fun translatedChapters(entry: Entry, chapters: List<EntryChapter>): Set<Long> {
        val source = sourceManager.get(entry.source) ?: return emptySet()
        return store().translatedChapters(chapters, entry, source)
    }

    override suspend fun translate(
        entry: Entry,
        chapter: EntryChapter,
        setup: EntryTranslateSetup,
        onProgress: (EntryTranslateProgress) -> Unit,
    ): EntryTranslateResult {
        val chapterSetup = setup.toChapterSetup()
            ?: return EntryTranslateResult.Failed(EntryTranslateFailure.SetupRequired)
        val source = sourceManager.get(entry.source)
            ?: return EntryTranslateResult.Failed(EntryTranslateFailure.DownloadMissing)
        val outcome = pages().use(chapter, entry, source) { chapterPages ->
            onProgress(EntryTranslateProgress(0, chapterPages.size))
            translator().translate(
                chapterPages,
                chapterSetup,
                mangaWorkContext(entry.title, entry.description),
            ) { done ->
                onProgress(EntryTranslateProgress(done, chapterPages.size))
            }
        } ?: return EntryTranslateResult.Failed(EntryTranslateFailure.DownloadMissing)
        return when (outcome) {
            is MangaChapterTranslationOutcome.Translated -> {
                store().write(chapter, entry, source, outcome.translation)
                EntryTranslateResult.Translated
            }
            MangaChapterTranslationOutcome.SetupRequired ->
                EntryTranslateResult.Failed(EntryTranslateFailure.SetupRequired)
            is MangaChapterTranslationOutcome.NothingRecognized ->
                EntryTranslateResult.Failed(EntryTranslateFailure.Error(outcome.reason))
        }
    }

    override suspend fun deleteTranslation(entry: Entry, chapters: List<EntryChapter>) {
        val source = sourceManager.get(entry.source) ?: return
        chapters.forEach { store().delete(it, entry, source) }
    }

    /** The setup as stored with the translation; `null` when it names no recognition pipeline. */
    private fun EntryTranslateSetup.toChapterSetup(): MangaChapterTranslationSetup? = MangaChapterTranslationSetup(
        pageLanguage = contentLanguage,
        pipeline = recognition ?: return null,
        route = ResolvedTranslationRoute(contentLanguage, targetLanguage, engine),
    )
}
