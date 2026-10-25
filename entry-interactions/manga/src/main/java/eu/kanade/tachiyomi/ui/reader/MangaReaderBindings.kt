package eu.kanade.tachiyomi.ui.reader

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoMap
import dev.zacsweers.metro.Provides
import dev.zacsweers.metrox.viewmodel.ViewModelAssistedFactory
import dev.zacsweers.metrox.viewmodel.ViewModelAssistedFactoryKey
import eu.kanade.tachiyomi.ui.reader.loader.ReaderPageSessionLoader
import kotlinx.coroutines.CoroutineScope
import mihon.core.metro.AppCoroutineScope
import mihon.entry.interactions.child.EntryChildListFeature
import mihon.entry.interactions.manga.download.DownloadCache
import mihon.entry.interactions.manga.download.DownloadManager
import mihon.entry.interactions.manga.download.DownloadProvider
import mihon.entry.interactions.manga.media.session.MangaMediaSessionProcessor
import mihon.entry.interactions.manga.page.MangaPageStore
import mihon.entry.interactions.manga.page.acquisition.MangaPageAcquisitionCoordinator
import mihon.entry.interactions.manga.translation.artifact.MangaChapterTranslationStore
import mihon.entry.interactions.reader.settings.MangaReaderSettings
import mihon.entry.interactions.source.EntryWebViewFeature
import mihon.entry.interactions.state.EntryBookmarkFeature
import mihon.entry.interactions.translate.EntryTranslateFeature
import mihon.entry.interactions.translation.EntryTranslationLanguagesFeature
import mihon.entry.viewer.settings.ViewerSettingBinder
import mihon.model.artifacts.api.ModelArtifactStore
import mihon.text.recognition.api.TextRecognitionFeature
import mihon.text.recognition.api.host.TextRecognitionHostActions
import mihon.translation.api.TranslationFeature
import mihon.translation.api.host.TranslationHostActions
import mihon.tts.api.TtsFeature
import tachiyomi.domain.entry.interactor.GetEntry
import tachiyomi.domain.entry.interactor.GetEntryWithChapters
import tachiyomi.domain.entry.repository.EntryChapterGroupRepository
import tachiyomi.domain.entry.repository.EntryProgressRepository
import tachiyomi.domain.entry.repository.EntryRepository
import tachiyomi.domain.entry.service.EntryCustomCoverWriter
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.source.local.image.LocalCoverManager

/**
 * The manga reader's page loading and view model. The view model is internal to this module, which a contributed
 * class can't be, so its factory joins the app's view model factories through this container.
 */
@ContributesTo(AppScope::class)
@BindingContainer
object MangaReaderBindings {
    @Provides
    internal fun pageSessionLoader(
        downloadManager: DownloadManager,
        downloadProvider: DownloadProvider,
        sourceManager: SourceManager,
        pageStore: MangaPageStore,
        pageAcquisition: MangaPageAcquisitionCoordinator,
        @AppCoroutineScope appScope: CoroutineScope,
    ): ReaderPageSessionLoader = ReaderPageSessionLoader(
        downloadManager = downloadManager,
        downloadProvider = downloadProvider,
        sourceManager = sourceManager,
        pageStore = pageStore,
        pageAcquisition = pageAcquisition,
        appScope = appScope,
    )

    @Provides
    @IntoMap
    @ViewModelAssistedFactoryKey(ReaderViewModel::class)
    internal fun readerViewModelFactory(
        application: Application,
        sourceManager: SourceManager,
        webViewFeature: EntryWebViewFeature,
        mangaReaderSettings: MangaReaderSettings,
        getEntry: GetEntry,
        getEntryWithChapters: GetEntryWithChapters,
        childListFeature: EntryChildListFeature,
        entryProgressRepository: EntryProgressRepository,
        entryChapterGroupRepository: EntryChapterGroupRepository,
        entryRepository: EntryRepository,
        bookmarkFeature: EntryBookmarkFeature,
        viewerSettingBinder: ViewerSettingBinder,
        libraryPreferences: LibraryPreferences,
        localCoverManager: LocalCoverManager,
        customCoverWriter: EntryCustomCoverWriter,
        mediaSession: MangaMediaSessionProcessor,
        @AppCoroutineScope appScope: CoroutineScope,
        downloadManager: DownloadManager,
        downloadCache: DownloadCache,
        pageSessionLoader: ReaderPageSessionLoader,
        chapterTranslationStore: MangaChapterTranslationStore,
        translationHostActions: TranslationHostActions,
        textRecognitionHostActions: TextRecognitionHostActions,
        translationFeature: TranslationFeature,
        translationLanguagesFeature: EntryTranslationLanguagesFeature,
        translateFeature: EntryTranslateFeature,
        ttsFeature: TtsFeature,
        textRecognitionFeature: TextRecognitionFeature,
        modelArtifactStore: ModelArtifactStore,
    ): ViewModelAssistedFactory = object : ViewModelAssistedFactory {
        override fun create(extras: CreationExtras): ViewModel = ReaderViewModel(
            savedState = extras.createSavedStateHandle(),
            application = application,
            sourceManager = sourceManager,
            webViewFeature = webViewFeature,
            mangaReaderSettings = mangaReaderSettings,
            getEntry = getEntry,
            getEntryWithChapters = getEntryWithChapters,
            childListFeature = childListFeature,
            entryProgressRepository = entryProgressRepository,
            entryChapterGroupRepository = entryChapterGroupRepository,
            entryRepository = entryRepository,
            bookmarkFeature = bookmarkFeature,
            viewerSettingBinder = viewerSettingBinder,
            libraryPreferences = libraryPreferences,
            localCoverManager = localCoverManager,
            customCoverWriter = customCoverWriter,
            mediaSession = mediaSession,
            appScope = appScope,
            downloadManager = downloadManager,
            downloadCache = downloadCache,
            pageSessionLoader = pageSessionLoader,
            chapterTranslationStore = chapterTranslationStore,
            translationHostActions = translationHostActions,
            textRecognitionHostActions = textRecognitionHostActions,
            translationFeature = translationFeature,
            translationLanguagesFeature = translationLanguagesFeature,
            translateFeature = translateFeature,
            ttsFeature = ttsFeature,
            textRecognitionFeature = textRecognitionFeature,
            modelArtifactStore = modelArtifactStore,
        )
    }
}
