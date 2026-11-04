package mihon.entry.interactions.manga.runtime

import android.app.Application
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlinx.serialization.json.Json
import mihon.core.archive.ArchiveOpener
import mihon.core.common.image.progressive.ProgressiveImageEngine
import mihon.entry.interactions.manga.download.DownloadProvider
import mihon.entry.interactions.manga.media.session.MangaMediaSessionProcessor
import mihon.entry.interactions.manga.page.MangaPageStore
import mihon.entry.interactions.manga.page.acquisition.MangaPageAcquisitionCoordinator
import mihon.entry.interactions.manga.reader.addMangaReaderImageComponents
import mihon.entry.interactions.manga.translation.artifact.MangaChapterTranslationStore
import mihon.entry.interactions.manga.translation.background.MangaChapterTranslationPreparer
import mihon.entry.interactions.manga.translation.background.MangaChapterTranslator
import mihon.entry.interactions.manga.translation.background.MangaEntryTranslateProcessor
import mihon.entry.interactions.manga.translation.pages.MangaDownloadedChapterPages
import mihon.entry.interactions.media.session.EntryMediaSessionEventSink
import mihon.entry.interactions.runtime.EntryImageComponentInstaller
import mihon.entry.interactions.runtime.EntryPageImageCache
import mihon.text.recognition.api.TextRecognitionFeature
import mihon.translation.api.TranslationFeature
import mihon.translation.api.host.TranslationHostActions
import tachiyomi.domain.source.service.SourceManager

@ContributesTo(AppScope::class)
@BindingContainer
object MangaEntryInteractionBindings {
    @Provides
    @SingleIn(AppScope::class)
    internal fun pageStore(application: Application, json: Json): MangaPageStore = MangaPageStore(application, json)

    @Provides
    @SingleIn(AppScope::class)
    internal fun pageAcquisitionCoordinator(
        store: MangaPageStore,
        progressiveImageEngine: ProgressiveImageEngine,
    ): MangaPageAcquisitionCoordinator = MangaPageAcquisitionCoordinator(store, progressiveImageEngine)

    @Provides
    internal fun pageImageCache(store: MangaPageStore): EntryPageImageCache = store

    @Provides
    @SingleIn(AppScope::class)
    internal fun chapterTranslationStore(provider: DownloadProvider): MangaChapterTranslationStore =
        MangaChapterTranslationStore(provider)

    @Provides
    @SingleIn(AppScope::class)
    internal fun mediaSession(sink: EntryMediaSessionEventSink): MangaMediaSessionProcessor =
        MangaMediaSessionProcessor(sink)

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
    @IntoSet
    fun imageComponents(): EntryImageComponentInstaller = EntryImageComponentInstaller(::addMangaReaderImageComponents)
}
