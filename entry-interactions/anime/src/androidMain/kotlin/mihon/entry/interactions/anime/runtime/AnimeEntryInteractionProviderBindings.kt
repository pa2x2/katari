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
import mihon.entry.interactions.anime.media.AnimeImmersiveProcessor
import mihon.entry.interactions.anime.media.AnimeMediaCacheProvider
import mihon.entry.interactions.anime.media.AnimePreviewInteraction
import mihon.entry.interactions.anime.media.session.AnimeMediaSessionProcessor
import mihon.entry.interactions.anime.navigation.AnimeOpenProcessor
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
import tachiyomi.domain.entry.repository.EntryProgressRepository

/**
 * Anime's contributions that need the Android player: opening, previews, the immersive view, the player's media
 * session events and its playback cache. Downloads are in [AnimeDownloadBindings] and the rest in
 * [AnimeEntryTypeBindings], which every platform has.
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
