package mihon.entry.interactions.manga.runtime

import android.app.Application
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.ui.reader.loader.ReaderPageSessionLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.json.Json
import mihon.core.common.image.progressive.ProgressiveImageEngine
import mihon.core.metro.AppCoroutineScope
import mihon.entry.interactions.download.EntryDownloadWorkController
import mihon.entry.interactions.manga.download.DownloadCache
import mihon.entry.interactions.manga.download.DownloadManager
import mihon.entry.interactions.manga.download.DownloadPendingDeleter
import mihon.entry.interactions.manga.download.DownloadProvider
import mihon.entry.interactions.manga.download.DownloadStore
import mihon.entry.interactions.manga.download.Downloader
import mihon.entry.interactions.manga.media.session.MangaMediaSessionProcessor
import mihon.entry.interactions.manga.page.MangaPageStore
import mihon.entry.interactions.manga.page.acquisition.MangaPageAcquisitionCoordinator
import mihon.entry.interactions.manga.reader.addMangaReaderImageComponents
import mihon.entry.interactions.manga.reader.settings.MangaReaderSettingsProvider
import mihon.entry.interactions.manga.translation.artifact.MangaChapterTranslationStore
import mihon.entry.interactions.manga.translation.background.MangaChapterTranslationPreparer
import mihon.entry.interactions.manga.translation.background.MangaChapterTranslator
import mihon.entry.interactions.manga.translation.background.MangaEntryTranslateProcessor
import mihon.entry.interactions.manga.translation.pages.MangaDownloadedChapterPages
import mihon.entry.interactions.media.DefaultEntryViewerSettingsProvider
import mihon.entry.interactions.media.ENTRY_VIEWER_SETTINGS_LEGACY_PREFERENCE_OWNER_GROUP_ID
import mihon.entry.interactions.media.session.EntryMediaSessionEventSink
import mihon.entry.interactions.reader.preparation.ReaderChapterPreparationPreferences
import mihon.entry.interactions.reader.settings.MangaReaderSettings
import mihon.entry.interactions.runtime.EntryImageComponentInstaller
import mihon.entry.interactions.runtime.EntryInteractionPlugin
import mihon.entry.interactions.runtime.EntryPageImageCache
import mihon.entry.interactions.runtime.ReaderSettingsSurfaceCapabilities
import mihon.entry.interactions.settings.EntryInteractionPreferences
import mihon.feature.runtime.FeatureRuntimeWarmup
import mihon.text.recognition.api.TextRecognitionFeature
import mihon.translation.api.TranslationFeature
import mihon.translation.api.host.TranslationHostActions
import nl.adaptivity.xmlutil.serialization.XML
import tachiyomi.core.common.preference.InstalledProfilePreferenceOwner
import tachiyomi.core.common.preference.ProfilePreferenceOwnerGroupId
import tachiyomi.core.common.preference.ProfilePreferenceOwnerId
import tachiyomi.core.common.preference.ProfilePreferenceOwnerInstaller
import tachiyomi.domain.category.interactor.GetCategories
import tachiyomi.domain.download.service.DownloadPreferences
import tachiyomi.domain.entry.interactor.GetEntryWithChapters
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.entry.repository.EntryProgressRepository
import tachiyomi.domain.entry.repository.EntryRepository
import tachiyomi.domain.library.service.GlobalLibraryPreferences
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.domain.storage.service.StorageManager
import tachiyomi.domain.track.interactor.GetTracks

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
    internal fun downloadProvider(
        application: Application,
        storageManager: StorageManager,
        libraryPreferences: GlobalLibraryPreferences,
    ): DownloadProvider = DownloadProvider(application, storageManager, libraryPreferences)

    @Provides
    @SingleIn(AppScope::class)
    internal fun downloader(
        application: Application,
        provider: DownloadProvider,
        cache: DownloadCache,
        sourceManager: SourceManager,
        pageImageCache: EntryPageImageCache,
        downloadPreferences: DownloadPreferences,
        xml: XML,
        getCategories: GetCategories,
        getTracks: GetTracks,
        json: Json,
        entryRepository: EntryRepository,
        entryChapterRepository: EntryChapterRepository,
        @AppCoroutineScope scope: CoroutineScope,
    ): Downloader = Downloader(
        context = application,
        provider = provider,
        cache = cache,
        sourceManager = sourceManager,
        pageImageCache = pageImageCache,
        downloadPreferences = downloadPreferences,
        xml = xml,
        getCategories = getCategories,
        getTracks = getTracks,
        scope = scope,
        store = DownloadStore(application, sourceManager, json, entryRepository, entryChapterRepository),
    )

    @Provides
    @SingleIn(AppScope::class)
    internal fun downloadManager(
        application: Application,
        provider: DownloadProvider,
        cache: DownloadCache,
        sourceManager: SourceManager,
        downloader: Downloader,
        json: Json,
        workController: EntryDownloadWorkController,
        @AppCoroutineScope scope: CoroutineScope,
    ): DownloadManager = DownloadManager(
        context = application,
        provider = provider,
        cache = cache,
        sourceManager = sourceManager,
        downloader = downloader,
        pendingDeleter = DownloadPendingDeleter(application, json),
        workController = workController,
        scope = scope,
    )

    @Provides
    @SingleIn(AppScope::class)
    internal fun downloadCache(
        application: Application,
        provider: DownloadProvider,
        sourceManager: SourceManager,
        storageManager: StorageManager,
        @AppCoroutineScope scope: CoroutineScope,
    ): DownloadCache = DownloadCache(application, provider, sourceManager, storageManager, scope)

    @Provides
    @SingleIn(AppScope::class)
    internal fun chapterTranslationStore(provider: DownloadProvider): MangaChapterTranslationStore =
        MangaChapterTranslationStore(provider)

    @Provides
    @SingleIn(AppScope::class)
    internal fun viewerSettingsOwner(
        owners: ProfilePreferenceOwnerInstaller,
        chapterPreparationPreferences: ReaderChapterPreparationPreferences,
    ): InstalledProfilePreferenceOwner<MangaReaderSettingsProvider> = owners.register(
        id = ProfilePreferenceOwnerId("entry-interactions.manga.viewer-settings"),
        groups = setOf(
            ProfilePreferenceOwnerGroupId(ENTRY_VIEWER_SETTINGS_LEGACY_PREFERENCE_OWNER_GROUP_ID),
        ),
        factory = { store -> MangaReaderSettingsProvider(store, chapterPreparationPreferences) },
    )

    @Provides
    @IntoSet
    internal fun viewerSettingsOwnerRegistration(
        owner: InstalledProfilePreferenceOwner<MangaReaderSettingsProvider>,
    ): InstalledProfilePreferenceOwner<*> = owner

    @Provides
    @SingleIn(AppScope::class)
    internal fun viewerSettingsProvider(
        owner: InstalledProfilePreferenceOwner<MangaReaderSettingsProvider>,
    ): MangaReaderSettingsProvider = owner.create()

    @Provides
    internal fun readerSettings(provider: MangaReaderSettingsProvider): MangaReaderSettings = provider

    @Provides
    @SingleIn(AppScope::class)
    internal fun mediaSession(sink: EntryMediaSessionEventSink): MangaMediaSessionProcessor =
        MangaMediaSessionProcessor(sink)

    @Provides
    internal fun translateProcessor(
        application: Application,
        sourceManager: SourceManager,
        store: () -> MangaChapterTranslationStore,
        downloadProvider: () -> DownloadProvider,
        recognition: () -> TextRecognitionFeature,
        translation: () -> TranslationFeature,
        hostActions: () -> TranslationHostActions,
    ): MangaEntryTranslateProcessor = MangaEntryTranslateProcessor(
        sourceManager = sourceManager,
        store = store,
        pages = { MangaDownloadedChapterPages(application, downloadProvider()) },
        translator = { MangaChapterTranslator(recognition(), translation()) },
        preparer = {
            MangaChapterTranslationPreparer(recognition(), translation()) {
                hostActions().defaultTarget()?.language
            }
        },
    )

    @Provides
    @IntoSet
    @SingleIn(AppScope::class)
    internal fun plugin(
        getEntryWithChapters: GetEntryWithChapters,
        entryChapterRepository: EntryChapterRepository,
        entryProgressRepository: EntryProgressRepository,
        downloadPreferences: DownloadPreferences,
        downloadManager: DownloadManager,
        downloadCache: DownloadCache,
        sourceManager: SourceManager,
        entryRepository: EntryRepository,
        mediaSession: MangaMediaSessionProcessor,
        entryInteractionPreferences: EntryInteractionPreferences,
        viewerSettingsProvider: MangaReaderSettingsProvider,
        pageSessionLoader: Lazy<ReaderPageSessionLoader>,
        translateProcessor: MangaEntryTranslateProcessor,
        pageImageCache: () -> EntryPageImageCache,
    ): EntryInteractionPlugin = mangaEntryInteractionPlugin(
        MangaEntryInteractionRuntimeDependencies(
            getEntryWithChapters = getEntryWithChapters,
            entryChapterRepository = entryChapterRepository,
            entryProgressRepository = entryProgressRepository,
            downloadPreferences = downloadPreferences,
            downloadManager = downloadManager,
            downloadCache = downloadCache,
            sourceManager = sourceManager,
            entryRepository = entryRepository,
            mediaSession = mediaSession,
            entryInteractionPreferences = entryInteractionPreferences,
        ),
        pageSessionLoader = pageSessionLoader,
        translateProcessor = translateProcessor,
        pageImageCache = pageImageCache,
        viewerSettingsProvider = DefaultEntryViewerSettingsProvider(
            type = EntryType.MANGA,
            surfaces = listOf(viewerSettingsProvider),
            legacyViewerFlagsNormalization = { flags -> flags and LEGACY_MANGA_VIEWER_MASK.inv() },
        ),
    )

    @Provides
    @IntoSet
    internal fun downloadManagerWarmup(downloadManager: () -> DownloadManager): FeatureRuntimeWarmup =
        FeatureRuntimeWarmup { downloadManager() }

    @Provides
    @IntoSet
    fun imageComponents(): EntryImageComponentInstaller = EntryImageComponentInstaller(::addMangaReaderImageComponents)

    @Provides
    @IntoSet
    fun readerSurfaceCapabilities(): ReaderSettingsSurfaceCapabilities = ReaderSettingsSurfaceCapabilities(
        surfaceId = MangaReaderSettings.PROVIDER_ID,
        capabilities = MangaReaderSettings.READER_CAPABILITIES,
    )
}

private const val LEGACY_MANGA_VIEWER_MASK = 0x3FL
