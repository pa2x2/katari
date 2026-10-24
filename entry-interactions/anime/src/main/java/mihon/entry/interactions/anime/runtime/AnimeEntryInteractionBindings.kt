package mihon.entry.interactions.anime.runtime

import android.app.Application
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.network.NetworkHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.json.Json
import mihon.core.metro.AppCoroutineScope
import mihon.core.metro.StartupWarmup
import mihon.entry.interactions.anime.download.AnimeDownloadCache
import mihon.entry.interactions.anime.download.AnimeDownloadManager
import mihon.entry.interactions.anime.download.AnimeDownloadProvider
import mihon.entry.interactions.anime.download.AnimeDownloadStore
import mihon.entry.interactions.anime.download.AnimeDownloader
import mihon.entry.interactions.anime.media.session.AnimeMediaSessionProcessor
import mihon.entry.interactions.anime.stream.StreamTypeProbe
import mihon.entry.interactions.catalogue.EntryCatalogueFeature
import mihon.entry.interactions.download.EntryDownloadWorkController
import mihon.entry.interactions.media.ENTRY_VIEWER_SETTINGS_LEGACY_PREFERENCE_OWNER_GROUP_ID
import mihon.entry.interactions.media.session.EntryMediaSessionEventSink
import mihon.entry.interactions.settings.AnimePlayerPreferences
import tachiyomi.core.common.preference.InstalledProfilePreferenceOwner
import tachiyomi.core.common.preference.ProfilePreferenceOwnerGroupId
import tachiyomi.core.common.preference.ProfilePreferenceOwnerId
import tachiyomi.core.common.preference.ProfilePreferenceOwnerInstaller
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.entry.repository.EntryRepository
import tachiyomi.domain.entry.repository.PlaybackPreferencesRepository
import tachiyomi.domain.library.service.GlobalLibraryPreferences
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.domain.storage.service.StorageManager

@ContributesTo(AppScope::class)
@BindingContainer
object AnimeEntryInteractionBindings {
    @Provides
    @SingleIn(AppScope::class)
    internal fun downloadProvider(
        application: Application,
        storageManager: StorageManager,
        libraryPreferences: GlobalLibraryPreferences,
        json: Json,
    ): AnimeDownloadProvider = AnimeDownloadProvider(application, storageManager, libraryPreferences, json)

    @Provides
    @SingleIn(AppScope::class)
    internal fun downloadCache(
        application: Application,
        provider: AnimeDownloadProvider,
        sourceManager: SourceManager,
        catalogueFeature: () -> EntryCatalogueFeature,
        storageManager: StorageManager,
        @AppCoroutineScope scope: CoroutineScope,
    ): AnimeDownloadCache = AnimeDownloadCache(
        context = application,
        provider = provider,
        sourceManager = sourceManager,
        catalogueFeature = catalogueFeature,
        storageManager = storageManager,
        scope = scope,
    )

    @Provides
    @SingleIn(AppScope::class)
    internal fun downloader(
        application: Application,
        provider: AnimeDownloadProvider,
        cache: AnimeDownloadCache,
        sourceManager: SourceManager,
        playbackPreferencesRepository: PlaybackPreferencesRepository,
        networkHelper: NetworkHelper,
        streamTypeProbe: StreamTypeProbe,
        json: Json,
    ): AnimeDownloader = AnimeDownloader(
        context = application,
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
        application: Application,
        cache: AnimeDownloadCache,
        provider: AnimeDownloadProvider,
        downloader: AnimeDownloader,
        sourceManager: SourceManager,
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
        store = AnimeDownloadStore(application, json, entryRepository, entryChapterRepository),
        workController = workController,
        scope = scope,
    )

    @Provides
    @SingleIn(AppScope::class)
    fun viewerSettingsOwner(
        owners: ProfilePreferenceOwnerInstaller,
    ): InstalledProfilePreferenceOwner<AnimePlayerPreferences> = owners.register(
        id = ProfilePreferenceOwnerId("entry-interactions.anime.viewer-settings"),
        groups = setOf(
            ProfilePreferenceOwnerGroupId(ENTRY_VIEWER_SETTINGS_LEGACY_PREFERENCE_OWNER_GROUP_ID),
        ),
        factory = ::AnimePlayerPreferences,
    )

    @Provides
    @IntoSet
    fun viewerSettingsOwnerRegistration(
        owner: InstalledProfilePreferenceOwner<AnimePlayerPreferences>,
    ): InstalledProfilePreferenceOwner<*> = owner

    @Provides
    @SingleIn(AppScope::class)
    fun viewerSettingsProvider(owner: InstalledProfilePreferenceOwner<AnimePlayerPreferences>): AnimePlayerPreferences =
        owner.create()

    @Provides
    @SingleIn(AppScope::class)
    internal fun mediaSession(sink: EntryMediaSessionEventSink): AnimeMediaSessionProcessor =
        AnimeMediaSessionProcessor(sink)

    @Provides
    @IntoSet
    internal fun downloadManagerWarmup(downloadManager: () -> AnimeDownloadManager): StartupWarmup =
        StartupWarmup { downloadManager() }
}
