package mihon.entry.interactions.manga.runtime

import android.app.Application
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.source.entry.EntryType
import kotlinx.serialization.json.Json
import mihon.core.common.image.progressive.ProgressiveImageEngine
import mihon.entry.interactions.manga.download.DownloadCache
import mihon.entry.interactions.manga.download.DownloadManager
import mihon.entry.interactions.manga.download.DownloadProvider
import mihon.entry.interactions.manga.media.session.MangaMediaSessionProcessor
import mihon.entry.interactions.manga.page.MangaPageStore
import mihon.entry.interactions.manga.page.acquisition.MangaPageAcquisitionCoordinator
import mihon.entry.interactions.manga.reader.addMangaReaderImageComponents
import mihon.entry.interactions.manga.reader.settings.MangaReaderSettingsProvider
import mihon.entry.interactions.manga.translation.artifact.MangaChapterTranslationStore
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
import mihon.feature.runtime.injekt.InjektExport
import mihon.feature.runtime.injekt.injektExport
import tachiyomi.core.common.preference.InstalledProfilePreferenceOwner
import tachiyomi.core.common.preference.ProfilePreferenceOwnerGroupId
import tachiyomi.core.common.preference.ProfilePreferenceOwnerId
import tachiyomi.core.common.preference.ProfilePreferenceOwnerInstaller
import tachiyomi.domain.download.service.DownloadPreferences
import tachiyomi.domain.entry.interactor.GetEntryWithChapters
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.entry.repository.EntryProgressRepository
import tachiyomi.domain.entry.repository.EntryRepository
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
    internal fun downloadProvider(application: Application): DownloadProvider = DownloadProvider(application)

    @Provides
    @SingleIn(AppScope::class)
    internal fun downloadManager(application: Application): DownloadManager = DownloadManager(application)

    @Provides
    @SingleIn(AppScope::class)
    internal fun downloadCache(application: Application): DownloadCache = DownloadCache(application)

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

    @Provides
    @IntoSet
    internal fun exportPageStore(store: () -> MangaPageStore): InjektExport<*> = injektExport(store)

    @Provides
    @IntoSet
    internal fun exportPageAcquisitionCoordinator(
        coordinator: () -> MangaPageAcquisitionCoordinator,
    ): InjektExport<*> = injektExport(coordinator)

    @Provides
    @IntoSet
    fun exportPageImageCache(cache: () -> EntryPageImageCache): InjektExport<*> = injektExport(cache)

    @Provides
    @IntoSet
    internal fun exportDownloadProvider(provider: () -> DownloadProvider): InjektExport<*> = injektExport(provider)

    @Provides
    @IntoSet
    internal fun exportDownloadManager(manager: () -> DownloadManager): InjektExport<*> = injektExport(manager)

    @Provides
    @IntoSet
    internal fun exportDownloadCache(cache: () -> DownloadCache): InjektExport<*> = injektExport(cache)

    @Provides
    @IntoSet
    internal fun exportChapterTranslationStore(store: () -> MangaChapterTranslationStore): InjektExport<*> =
        injektExport(store)

    @Provides
    @IntoSet
    internal fun exportViewerSettingsProvider(provider: () -> MangaReaderSettingsProvider): InjektExport<*> =
        injektExport(provider)

    @Provides
    @IntoSet
    fun exportReaderSettings(settings: () -> MangaReaderSettings): InjektExport<*> = injektExport(settings)

    @Provides
    @IntoSet
    internal fun exportMediaSession(mediaSession: () -> MangaMediaSessionProcessor): InjektExport<*> =
        injektExport(mediaSession)
}

private const val LEGACY_MANGA_VIEWER_MASK = 0x3FL
