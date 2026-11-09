package app.katari.entry.interactions.manga.runtime

import app.katari.core.archive.ArchiveOpener
import app.katari.entry.interactions.manga.download.DownloadProvider
import app.katari.entry.interactions.manga.translation.artifact.MangaChapterTranslationStore
import app.katari.entry.interactions.manga.translation.background.MangaChapterTranslationPreparer
import app.katari.entry.interactions.manga.translation.background.MangaChapterTranslator
import app.katari.entry.interactions.manga.translation.background.MangaEntryTranslateProcessor
import app.katari.entry.interactions.manga.translation.pages.MangaDownloadedChapterPages
import app.katari.entry.interactions.runtime.EntryTypeKey
import app.katari.entry.interactions.translate.EntryTranslateProcessor
import app.katari.text.recognition.api.TextRecognitionFeature
import app.katari.translation.api.TranslationFeature
import app.katari.translation.api.host.TranslationHostActions
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoMap
import dev.zacsweers.metro.Provides
import eu.kanade.tachiyomi.source.entry.EntryType
import tachiyomi.domain.source.service.SourceManager

/** Translating downloaded manga chapters, which every platform with text recognition does the same way. */
@ContributesTo(AppScope::class)
@BindingContainer
object MangaTranslateBindings {
    @Provides
    internal fun translateProcessor(
        archives: ArchiveOpener,
        sourceManager: SourceManager,
        store: () -> MangaChapterTranslationStore,
        downloadProvider: () -> DownloadProvider,
        recognition: () -> TextRecognitionFeature,
        translation: () -> TranslationFeature,
        hostActions: () -> TranslationHostActions,
    ): MangaEntryTranslateProcessor = MangaEntryTranslateProcessor(
        sourceManager = sourceManager,
        store = store,
        pages = { MangaDownloadedChapterPages(archives, downloadProvider()) },
        translator = { MangaChapterTranslator(recognition(), translation()) },
        preparer = {
            MangaChapterTranslationPreparer(recognition(), translation()) {
                hostActions().defaultTarget()?.language
            }
        },
    )

    @Provides
    @IntoMap
    @EntryTypeKey(EntryType.MANGA)
    internal fun translate(processor: MangaEntryTranslateProcessor): EntryTranslateProcessor = processor
}
