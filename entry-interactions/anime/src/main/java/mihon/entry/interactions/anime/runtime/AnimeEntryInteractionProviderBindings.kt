package mihon.entry.interactions.anime.runtime

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoMap
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.ui.video.player.AnimePlayerBasePreferences
import eu.kanade.tachiyomi.ui.video.player.VideoStreamResolver
import eu.kanade.tachiyomi.ui.video.player.engine.VideoEngine
import mihon.entry.interactions.anime.child.AnimeChildListProcessor
import mihon.entry.interactions.anime.download.AnimeDownloadCache
import mihon.entry.interactions.anime.download.AnimeDownloadManager
import mihon.entry.interactions.anime.download.AnimeDownloadProcessor
import mihon.entry.interactions.anime.library.AnimeLibraryProgressProvider
import mihon.entry.interactions.anime.media.AnimeImmersiveProcessor
import mihon.entry.interactions.anime.media.AnimeMediaCacheProvider
import mihon.entry.interactions.anime.media.AnimePlaybackPreferencesProcessor
import mihon.entry.interactions.anime.media.AnimePreviewInteraction
import mihon.entry.interactions.anime.media.session.AnimeMediaSessionProcessor
import mihon.entry.interactions.anime.migration.AnimeMigrationProvider
import mihon.entry.interactions.anime.navigation.AnimeContinueProcessor
import mihon.entry.interactions.anime.navigation.AnimeOpenProcessor
import mihon.entry.interactions.anime.presentation.AnimeEntryTypePresentationProvider
import mihon.entry.interactions.anime.state.AnimeConsumptionProcessor
import mihon.entry.interactions.anime.state.AnimeProgressProcessor
import mihon.entry.interactions.anime.statistics.AnimeEntryStatisticsProvider
import mihon.entry.interactions.download.EntryBulkDownloadCandidateProcessor
import mihon.entry.interactions.download.EntryDownloadOptionsProcessor
import mihon.entry.interactions.download.EntryDownloadProcessor
import mihon.entry.interactions.library.EntryLibraryProgressProvider
import mihon.entry.interactions.media.DefaultEntryViewerSettingsProvider
import mihon.entry.interactions.media.EntryMediaCacheProvider
import mihon.entry.interactions.media.EntryMediaSessionProcessor
import mihon.entry.interactions.media.EntryViewerSettingsProvider
import mihon.entry.interactions.navigation.EntryContinueProcessor
import mihon.entry.interactions.navigation.EntryOpenProcessor
import mihon.entry.interactions.runtime.EntryChildListProcessor
import mihon.entry.interactions.runtime.EntryChildProgressProcessor
import mihon.entry.interactions.runtime.EntryImmersiveProcessor
import mihon.entry.interactions.runtime.EntryPlayerCache
import mihon.entry.interactions.runtime.EntryPreviewConfigurationProvider
import mihon.entry.interactions.runtime.EntryPreviewProcessor
import mihon.entry.interactions.runtime.EntryStatisticsProvider
import mihon.entry.interactions.runtime.EntryTypeKey
import mihon.entry.interactions.runtime.EntryTypePresentationProvider
import mihon.entry.interactions.settings.AnimePlayerPreferences
import mihon.entry.interactions.settings.EntryInteractionPreferences
import mihon.entry.interactions.state.EntryConsumptionProcessor
import mihon.entry.interactions.state.EntryMigrationProvider
import mihon.entry.interactions.state.EntryPlaybackPreferencesProcessor
import mihon.entry.interactions.state.EntryProgressProcessor
import tachiyomi.domain.entry.interactor.GetEntryWithChapters
import tachiyomi.domain.entry.repository.DownloadPreferencesRepository
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.entry.repository.EntryProgressRepository
import tachiyomi.domain.entry.repository.EntryRepository
import tachiyomi.domain.entry.repository.PlaybackPreferencesRepository
import tachiyomi.domain.source.service.SourceManager

@ContributesTo(AppScope::class)
@BindingContainer
object AnimeEntryInteractionProviderBindings {
    @Provides
    @SingleIn(AppScope::class)
    internal fun openProcessor(): AnimeOpenProcessor = AnimeOpenProcessor()

    @Provides
    @IntoMap
    @EntryTypeKey(EntryType.ANIME)
    internal fun open(processor: AnimeOpenProcessor): EntryOpenProcessor = processor

    @Provides
    @IntoMap
    @EntryTypeKey(EntryType.ANIME)
    @SingleIn(AppScope::class)
    internal fun continueEntry(
        getEntryWithChapters: GetEntryWithChapters,
        entryProgressRepository: EntryProgressRepository,
        openProcessor: AnimeOpenProcessor,
    ): EntryContinueProcessor = AnimeContinueProcessor(getEntryWithChapters, entryProgressRepository, openProcessor)

    @Provides
    @IntoMap
    @EntryTypeKey(EntryType.ANIME)
    @SingleIn(AppScope::class)
    internal fun consumption(entryProgressRepository: EntryProgressRepository): EntryConsumptionProcessor =
        AnimeConsumptionProcessor(entryProgressRepository)

    @Provides
    @IntoMap
    @EntryTypeKey(EntryType.ANIME)
    @SingleIn(AppScope::class)
    internal fun progress(
        entryProgressRepository: EntryProgressRepository,
        entryChapterRepository: EntryChapterRepository,
    ): EntryProgressProcessor = AnimeProgressProcessor(entryProgressRepository, entryChapterRepository)

    @Provides
    @IntoMap
    @EntryTypeKey(EntryType.ANIME)
    @SingleIn(AppScope::class)
    internal fun playbackPreferences(
        playbackPreferencesRepository: PlaybackPreferencesRepository,
    ): EntryPlaybackPreferencesProcessor = AnimePlaybackPreferencesProcessor(playbackPreferencesRepository)

    @Provides
    @SingleIn(AppScope::class)
    internal fun downloadProcessor(
        downloadManager: AnimeDownloadManager,
        downloadCache: AnimeDownloadCache,
        downloadPreferencesRepository: DownloadPreferencesRepository,
        sourceManager: SourceManager,
        entryRepository: EntryRepository,
        entryChapterRepository: EntryChapterRepository,
    ): AnimeDownloadProcessor = AnimeDownloadProcessor(
        animeDownloadManager = downloadManager,
        animeDownloadCache = downloadCache,
        downloadPreferencesRepository = downloadPreferencesRepository,
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
    @IntoMap
    @EntryTypeKey(EntryType.ANIME)
    internal fun migration(): EntryMigrationProvider = AnimeMigrationProvider()

    @Provides
    @SingleIn(AppScope::class)
    internal fun childListProcessor(entryProgressRepository: EntryProgressRepository): AnimeChildListProcessor =
        AnimeChildListProcessor(entryProgressRepository)

    @Provides
    @IntoMap
    @EntryTypeKey(EntryType.ANIME)
    internal fun childList(processor: AnimeChildListProcessor): EntryChildListProcessor = processor

    @Provides
    @IntoMap
    @EntryTypeKey(EntryType.ANIME)
    internal fun childProgress(processor: AnimeChildListProcessor): EntryChildProgressProcessor = processor

    @Provides
    @IntoMap
    @EntryTypeKey(EntryType.ANIME)
    @SingleIn(AppScope::class)
    internal fun libraryProgress(entryProgressRepository: EntryProgressRepository): EntryLibraryProgressProvider =
        AnimeLibraryProgressProvider(entryProgressRepository)

    @Provides
    @SingleIn(AppScope::class)
    internal fun previewProcessor(entryInteractionPreferences: EntryInteractionPreferences): AnimePreviewInteraction =
        AnimePreviewInteraction(entryInteractionPreferences)

    @Provides
    @IntoMap
    @EntryTypeKey(EntryType.ANIME)
    internal fun preview(processor: AnimePreviewInteraction): EntryPreviewProcessor = processor

    @Provides
    @IntoMap
    @EntryTypeKey(EntryType.ANIME)
    internal fun previewConfiguration(processor: AnimePreviewInteraction): EntryPreviewConfigurationProvider =
        processor

    @Provides
    @IntoMap
    @EntryTypeKey(EntryType.ANIME)
    @SingleIn(AppScope::class)
    internal fun immersive(
        entryProgressRepository: EntryProgressRepository,
        resolveVideoStream: () -> VideoStreamResolver,
        mediaSession: AnimeMediaSessionProcessor,
        videoEngine: () -> VideoEngine,
        basePreferences: () -> AnimePlayerBasePreferences,
    ): EntryImmersiveProcessor = AnimeImmersiveProcessor(
        entryProgressRepository = entryProgressRepository,
        resolveVideoStream = resolveVideoStream,
        mediaSession = mediaSession,
        videoEngine = videoEngine,
        basePreferences = basePreferences,
    )

    @Provides
    @IntoMap
    @EntryTypeKey(EntryType.ANIME)
    internal fun mediaSession(processor: AnimeMediaSessionProcessor): EntryMediaSessionProcessor = processor

    @Provides
    @IntoMap
    @EntryTypeKey(EntryType.ANIME)
    internal fun typePresentation(): EntryTypePresentationProvider = AnimeEntryTypePresentationProvider

    @Provides
    @IntoMap
    @EntryTypeKey(EntryType.ANIME)
    internal fun statistics(): EntryStatisticsProvider = AnimeEntryStatisticsProvider

    @Provides
    @IntoMap
    @EntryTypeKey(EntryType.ANIME)
    internal fun mediaCache(playerCache: () -> EntryPlayerCache): EntryMediaCacheProvider =
        AnimeMediaCacheProvider(playerCache)

    @Provides
    @IntoMap
    @EntryTypeKey(EntryType.ANIME)
    @SingleIn(AppScope::class)
    internal fun viewerSettings(preferences: AnimePlayerPreferences): EntryViewerSettingsProvider =
        DefaultEntryViewerSettingsProvider(surfaces = listOf(preferences))
}
