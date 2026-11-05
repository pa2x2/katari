package mihon.entry.interactions.manga.runtime

import android.app.Application
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import mihon.core.archive.ArchiveOpener
import mihon.entry.interactions.manga.download.DownloadProvider
import mihon.entry.interactions.manga.reader.image.export.AndroidMangaPageImageExport
import mihon.entry.interactions.manga.reader.image.export.MangaPageImageExport
import mihon.entry.interactions.manga.translation.artifact.MangaChapterTranslationStore
import mihon.entry.interactions.manga.translation.background.MangaChapterTranslationPreparer
import mihon.entry.interactions.manga.translation.background.MangaChapterTranslator
import mihon.entry.interactions.manga.translation.background.MangaEntryTranslateProcessor
import mihon.entry.interactions.manga.translation.pages.MangaDownloadedChapterPages
import mihon.text.recognition.api.TextRecognitionFeature
import mihon.translation.api.TranslationFeature
import mihon.translation.api.host.TranslationHostActions
import tachiyomi.domain.source.service.SourceManager

@ContributesTo(AppScope::class)
@BindingContainer
object MangaEntryInteractionBindings {
    @Provides
    @SingleIn(AppScope::class)
    internal fun pageImageExport(application: Application): MangaPageImageExport =
        AndroidMangaPageImageExport(application)

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
}
