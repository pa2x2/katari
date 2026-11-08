package eu.kanade.tachiyomi.ui.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import app.katari.core.archive.ArchiveOpener
import app.katari.core.metro.AppCoroutineScope
import app.katari.core.platform.PlatformContext
import app.katari.core.platform.network.NetworkConnectivity
import app.katari.core.platform.power.PowerSource
import app.katari.entry.interactions.child.EntryChildListFeature
import app.katari.entry.interactions.manga.download.DownloadCache
import app.katari.entry.interactions.manga.download.DownloadManager
import app.katari.entry.interactions.manga.download.DownloadProvider
import app.katari.entry.interactions.manga.media.session.MangaMediaSessionProcessor
import app.katari.entry.interactions.manga.page.MangaPageStore
import app.katari.entry.interactions.manga.page.acquisition.MangaPageAcquisitionCoordinator
import app.katari.entry.interactions.manga.translation.artifact.MangaChapterTranslationStore
import app.katari.entry.interactions.media.handoff.EntryProgressOffers
import app.katari.entry.interactions.reader.image.export.ReaderImageExport
import app.katari.entry.interactions.reader.settings.MangaReaderSettings
import app.katari.entry.interactions.source.EntryWebViewFeature
import app.katari.entry.interactions.state.EntryBookmarkFeature
import app.katari.entry.interactions.translate.EntryTranslateFeature
import app.katari.entry.interactions.translation.EntryTranslationLanguagesFeature
import app.katari.entry.viewer.settings.ViewerSettingBinder
import app.katari.model.artifacts.api.ModelArtifactStore
import app.katari.text.recognition.api.TextRecognitionFeature
import app.katari.text.recognition.api.host.TextRecognitionHostActions
import app.katari.translation.api.TranslationFeature
import app.katari.translation.api.host.TranslationHostActions
import app.katari.tts.api.TtsFeature
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoMap
import dev.zacsweers.metro.Provides
import dev.zacsweers.metrox.viewmodel.ViewModelAssistedFactory
import dev.zacsweers.metrox.viewmodel.ViewModelAssistedFactoryKey
import eu.kanade.tachiyomi.ui.reader.loader.ReaderPageSessionLoader
import kotlinx.coroutines.CoroutineScope
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
        archives: ArchiveOpener,
        @AppCoroutineScope appScope: CoroutineScope,
    ): ReaderPageSessionLoader = ReaderPageSessionLoader(
        archives = archives,
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
        appContext: PlatformContext,
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
        networkConnectivity: NetworkConnectivity,
        powerSource: PowerSource,
        pageImageExport: ReaderImageExport,
        progressOffers: EntryProgressOffers,
    ): ViewModelAssistedFactory = object : ViewModelAssistedFactory {
        override fun create(extras: CreationExtras): ViewModel = ReaderViewModel(
            savedState = extras.createSavedStateHandle(),
            appContext = appContext,
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
            networkConnectivity = networkConnectivity,
            powerSource = powerSource,
            pageImageExport = pageImageExport,
            progressOffers = progressOffers,
        )
    }
}
