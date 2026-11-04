package mihon.entry.interactions.manga.runtime

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.ElementsIntoSet
import dev.zacsweers.metro.IntoMap
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.source.entry.EntryType
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.json.Json
import mihon.core.metro.AppCoroutineScope
import mihon.core.metro.StartupWarmup
import mihon.core.platform.PlatformContext
import mihon.core.platform.storage.AppPrivateDirectories
import mihon.entry.interactions.download.EntryBulkDownloadCandidateProcessor
import mihon.entry.interactions.download.EntryDownloadProcessor
import mihon.entry.interactions.download.EntryDownloadSetting
import mihon.entry.interactions.download.EntryDownloadWorkController
import mihon.entry.interactions.manga.download.DownloadCache
import mihon.entry.interactions.manga.download.DownloadManager
import mihon.entry.interactions.manga.download.DownloadPendingDeleter
import mihon.entry.interactions.manga.download.DownloadProvider
import mihon.entry.interactions.manga.download.DownloadStore
import mihon.entry.interactions.manga.download.Downloader
import mihon.entry.interactions.manga.download.MangaDownloadProcessor
import mihon.entry.interactions.runtime.EntryPageImageCache
import mihon.entry.interactions.runtime.EntryTypeKey
import nl.adaptivity.xmlutil.serialization.XML
import tachiyomi.core.common.preference.PlatformPreferenceFile
import tachiyomi.core.common.preference.PlatformPreferenceFiles
import tachiyomi.core.common.storage.MediaScanner
import tachiyomi.core.common.storage.StorageFileResolver
import tachiyomi.domain.category.interactor.GetCategories
import tachiyomi.domain.download.service.DownloadPreferences
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.entry.repository.EntryRepository
import tachiyomi.domain.library.service.GlobalLibraryPreferences
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.domain.storage.service.StorageManager
import tachiyomi.domain.track.interactor.GetTracks

@ContributesTo(AppScope::class)
@BindingContainer
object MangaDownloadBindings {
    @Provides
    @SingleIn(AppScope::class)
    internal fun downloadProvider(
        context: PlatformContext,
        storageManager: StorageManager,
        libraryPreferences: GlobalLibraryPreferences,
    ): DownloadProvider = DownloadProvider(context, storageManager, libraryPreferences)

    @Provides
    @SingleIn(AppScope::class)
    internal fun downloader(
        context: PlatformContext,
        provider: DownloadProvider,
        cache: DownloadCache,
        sourceManager: SourceManager,
        pageImageCache: EntryPageImageCache,
        mediaScanner: MediaScanner,
        downloadPreferences: DownloadPreferences,
        xml: XML,
        getCategories: GetCategories,
        getTracks: GetTracks,
        preferenceFiles: PlatformPreferenceFiles,
        json: Json,
        entryRepository: EntryRepository,
        entryChapterRepository: EntryChapterRepository,
        @AppCoroutineScope scope: CoroutineScope,
    ): Downloader = Downloader(
        context = context,
        provider = provider,
        cache = cache,
        sourceManager = sourceManager,
        pageImageCache = pageImageCache,
        mediaScanner = mediaScanner,
        downloadPreferences = downloadPreferences,
        xml = xml,
        getCategories = getCategories,
        getTracks = getTracks,
        scope = scope,
        store = DownloadStore(
            preferences = preferenceFiles.open(PlatformPreferenceFile.MANGA_DOWNLOAD_QUEUE),
            sourceManager = sourceManager,
            json = json,
            entryRepository = entryRepository,
            entryChapterRepository = entryChapterRepository,
        ),
    )

    @Provides
    @SingleIn(AppScope::class)
    internal fun downloadManager(
        context: PlatformContext,
        provider: DownloadProvider,
        cache: DownloadCache,
        sourceManager: SourceManager,
        downloader: Downloader,
        preferenceFiles: PlatformPreferenceFiles,
        json: Json,
        workController: EntryDownloadWorkController,
        @AppCoroutineScope scope: CoroutineScope,
    ): DownloadManager = DownloadManager(
        context = context,
        provider = provider,
        cache = cache,
        sourceManager = sourceManager,
        downloader = downloader,
        pendingDeleter = DownloadPendingDeleter(
            preferences = preferenceFiles.open(PlatformPreferenceFile.MANGA_CHAPTERS_TO_DELETE),
            json = json,
        ),
        workController = workController,
        scope = scope,
    )

    @Provides
    @SingleIn(AppScope::class)
    internal fun downloadCache(
        directories: AppPrivateDirectories,
        files: StorageFileResolver,
        provider: DownloadProvider,
        sourceManager: SourceManager,
        storageManager: StorageManager,
        @AppCoroutineScope scope: CoroutineScope,
    ): DownloadCache = DownloadCache(directories, files, provider, sourceManager, storageManager, scope)

    @Provides
    @SingleIn(AppScope::class)
    internal fun downloadProcessor(
        downloadManager: DownloadManager,
        downloadCache: DownloadCache,
        sourceManager: SourceManager,
        entryRepository: EntryRepository,
        entryChapterRepository: EntryChapterRepository,
    ): MangaDownloadProcessor = MangaDownloadProcessor(
        downloadManager = downloadManager,
        downloadCache = downloadCache,
        sourceManager = sourceManager,
        entryRepository = entryRepository,
        entryChapterRepository = entryChapterRepository,
    )

    @Provides
    @IntoMap
    @EntryTypeKey(EntryType.MANGA)
    internal fun download(processor: MangaDownloadProcessor): EntryDownloadProcessor = processor

    @Provides
    @IntoMap
    @EntryTypeKey(EntryType.MANGA)
    internal fun bulkDownloadCandidates(processor: MangaDownloadProcessor): EntryBulkDownloadCandidateProcessor =
        processor

    @Provides
    @ElementsIntoSet
    internal fun downloadSettings(): Set<EntryDownloadSetting> = Downloader.settings

    @Provides
    @IntoSet
    internal fun downloadManagerWarmup(downloadManager: () -> DownloadManager): StartupWarmup =
        StartupWarmup { downloadManager() }
}
