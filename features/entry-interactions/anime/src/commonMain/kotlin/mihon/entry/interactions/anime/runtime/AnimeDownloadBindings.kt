package mihon.entry.interactions.anime.runtime

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoMap
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.source.entry.EntryType
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.json.Json
import mihon.core.metro.AppCoroutineScope
import mihon.core.metro.StartupWarmup
import mihon.core.platform.PlatformContext
import mihon.core.platform.storage.AppPrivateDirectories
import mihon.entry.interactions.anime.download.AnimeDownloadCache
import mihon.entry.interactions.anime.download.AnimeDownloadManager
import mihon.entry.interactions.anime.download.AnimeDownloadProcessor
import mihon.entry.interactions.anime.download.AnimeDownloadProvider
import mihon.entry.interactions.anime.download.AnimeDownloadStore
import mihon.entry.interactions.anime.download.AnimeDownloader
import mihon.entry.interactions.anime.stream.StreamTypeProbe
import mihon.entry.interactions.catalogue.EntryCatalogueFeature
import mihon.entry.interactions.download.EntryBulkDownloadCandidateProcessor
import mihon.entry.interactions.download.EntryDownloadOptionsProcessor
import mihon.entry.interactions.download.EntryDownloadProcessor
import mihon.entry.interactions.download.EntryDownloadWorkController
import mihon.entry.interactions.runtime.EntryTypeKey
import tachiyomi.core.common.preference.PlatformPreferenceFile
import tachiyomi.core.common.preference.PlatformPreferenceFiles
import tachiyomi.core.common.storage.StorageFileResolver
import tachiyomi.domain.entry.repository.DownloadPreferencesRepository
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.entry.repository.EntryRepository
import tachiyomi.domain.entry.repository.PlaybackPreferencesRepository
import tachiyomi.domain.library.service.GlobalLibraryPreferences
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.domain.storage.service.StorageManager

@ContributesTo(AppScope::class)
@BindingContainer
object AnimeDownloadBindings {
    @Provides
    @SingleIn(AppScope::class)
    internal fun downloadProvider(
        context: PlatformContext,
        storageManager: StorageManager,
        libraryPreferences: GlobalLibraryPreferences,
        json: Json,
    ): AnimeDownloadProvider = AnimeDownloadProvider(context, storageManager, libraryPreferences, json)

    @Provides
    @SingleIn(AppScope::class)
    internal fun downloadCache(
        directories: AppPrivateDirectories,
        files: StorageFileResolver,
        provider: AnimeDownloadProvider,
        sourceManager: SourceManager,
        catalogueFeature: () -> EntryCatalogueFeature,
        storageManager: StorageManager,
        @AppCoroutineScope scope: CoroutineScope,
    ): AnimeDownloadCache = AnimeDownloadCache(
        directories = directories,
        files = files,
        provider = provider,
        sourceManager = sourceManager,
        catalogueFeature = catalogueFeature,
        storageManager = storageManager,
        scope = scope,
    )

    @Provides
    @SingleIn(AppScope::class)
    internal fun streamTypeProbe(networkHelper: NetworkHelper): StreamTypeProbe =
        StreamTypeProbe(networkHelper.client, networkHelper::defaultUserAgentProvider)

    @Provides
    @SingleIn(AppScope::class)
    internal fun downloader(
        files: StorageFileResolver,
        provider: AnimeDownloadProvider,
        cache: AnimeDownloadCache,
        sourceManager: SourceManager,
        playbackPreferencesRepository: PlaybackPreferencesRepository,
        networkHelper: NetworkHelper,
        streamTypeProbe: StreamTypeProbe,
        json: Json,
    ): AnimeDownloader = AnimeDownloader(
        files = files,
        provider = provider,
        cache = cache,
        sourceManager = sourceManager,
        playbackPreferencesRepository = playbackPreferencesRepository,
        networkHelper = networkHelper,
        streamTypeProbe = streamTypeProbe,
        json = json,
    )

    @Provides
    @SingleIn(AppScope::class)
    internal fun downloadManager(
        cache: AnimeDownloadCache,
        provider: AnimeDownloadProvider,
        downloader: AnimeDownloader,
        sourceManager: SourceManager,
        preferenceFiles: PlatformPreferenceFiles,
        json: Json,
        entryRepository: EntryRepository,
        entryChapterRepository: EntryChapterRepository,
        workController: EntryDownloadWorkController,
        @AppCoroutineScope scope: CoroutineScope,
    ): AnimeDownloadManager = AnimeDownloadManager(
        cache = cache,
        provider = provider,
        downloader = downloader,
        sourceManager = sourceManager,
        store = AnimeDownloadStore(
            preferences = preferenceFiles.open(PlatformPreferenceFile.ANIME_DOWNLOAD_QUEUE),
            json = json,
            entryRepository = entryRepository,
            entryChapterRepository = entryChapterRepository,
        ),
        workController = workController,
        scope = scope,
    )

    @Provides
    @SingleIn(AppScope::class)
    internal fun downloadProcessor(
        downloadManager: AnimeDownloadManager,
        downloadCache: AnimeDownloadCache,
        downloadPreferencesRepository: DownloadPreferencesRepository,
        playbackPreferencesRepository: PlaybackPreferencesRepository,
        sourceManager: SourceManager,
        entryRepository: EntryRepository,
        entryChapterRepository: EntryChapterRepository,
    ): AnimeDownloadProcessor = AnimeDownloadProcessor(
        animeDownloadManager = downloadManager,
        animeDownloadCache = downloadCache,
        downloadPreferencesRepository = downloadPreferencesRepository,
        playbackPreferencesRepository = playbackPreferencesRepository,
        sourceManager = sourceManager,
        entryRepository = entryRepository,
        entryChapterRepository = entryChapterRepository,
    )

    @Provides
    @IntoMap
    @EntryTypeKey(EntryType.ANIME)
    internal fun download(processor: AnimeDownloadProcessor): EntryDownloadProcessor = processor

    @Provides
    @IntoMap
    @EntryTypeKey(EntryType.ANIME)
    internal fun downloadOptions(processor: AnimeDownloadProcessor): EntryDownloadOptionsProcessor = processor

    @Provides
    @IntoMap
    @EntryTypeKey(EntryType.ANIME)
    internal fun bulkDownloadCandidates(processor: AnimeDownloadProcessor): EntryBulkDownloadCandidateProcessor =
        processor

    @Provides
    @IntoSet
    internal fun downloadManagerWarmup(downloadManager: () -> AnimeDownloadManager): StartupWarmup =
        StartupWarmup { downloadManager() }
}
