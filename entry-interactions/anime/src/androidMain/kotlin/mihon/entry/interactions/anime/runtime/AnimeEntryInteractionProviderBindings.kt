package mihon.entry.interactions.anime.runtime

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoMap
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.ui.video.player.AnimePlayerBasePreferences
import eu.kanade.tachiyomi.ui.video.player.VideoStreamResolver
import mihon.entry.interactions.anime.download.AnimeDownloadCache
import mihon.entry.interactions.anime.download.AnimeDownloadManager
import mihon.entry.interactions.anime.download.AnimeDownloadProcessor
import mihon.entry.interactions.anime.media.AnimeImmersiveProcessor
import mihon.entry.interactions.anime.media.AnimeMediaCacheProvider
import mihon.entry.interactions.anime.media.AnimePreviewInteraction
import mihon.entry.interactions.anime.media.session.AnimeMediaSessionProcessor
import mihon.entry.interactions.anime.navigation.AnimeOpenProcessor
import mihon.entry.interactions.download.EntryBulkDownloadCandidateProcessor
import mihon.entry.interactions.download.EntryDownloadOptionsProcessor
import mihon.entry.interactions.download.EntryDownloadProcessor
import mihon.entry.interactions.media.EntryMediaCacheProvider
import mihon.entry.interactions.media.EntryMediaSessionProcessor
import mihon.entry.interactions.navigation.EntryOpenProcessor
import mihon.entry.interactions.runtime.EntryImmersiveProcessor
import mihon.entry.interactions.runtime.EntryPlayerCache
import mihon.entry.interactions.runtime.EntryPreviewConfigurationProvider
import mihon.entry.interactions.runtime.EntryPreviewProcessor
import mihon.entry.interactions.runtime.EntryTypeKey
import mihon.entry.interactions.settings.EntryInteractionPreferences
import mihon.video.engine.spi.VideoEngine
import tachiyomi.domain.entry.repository.DownloadPreferencesRepository
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.entry.repository.EntryProgressRepository
import tachiyomi.domain.entry.repository.EntryRepository
import tachiyomi.domain.entry.repository.PlaybackPreferencesRepository
import tachiyomi.domain.source.service.SourceManager

/**
 * Anime's contributions that need the Android player or downloads: opening, downloads, previews, the immersive view,
 * the player's media session events and its playback cache. The rest is in [AnimeEntryTypeBindings], which every
 * platform has.
 */
@ContributesTo(AppScope::class)
@BindingContainer
object AnimeEntryInteractionProviderBindings {
    @Provides
    @IntoMap
    @EntryTypeKey(EntryType.ANIME)
    internal fun open(): EntryOpenProcessor = AnimeOpenProcessor()

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
    internal fun mediaCache(playerCache: () -> EntryPlayerCache): EntryMediaCacheProvider =
        AnimeMediaCacheProvider(playerCache)
}
