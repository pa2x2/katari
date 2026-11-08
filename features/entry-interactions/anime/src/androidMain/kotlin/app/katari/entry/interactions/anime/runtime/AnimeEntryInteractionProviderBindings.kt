package app.katari.entry.interactions.anime.runtime

import app.katari.entry.interactions.anime.media.AnimeMediaCacheProvider
import app.katari.entry.interactions.anime.media.AnimePreviewInteraction
import app.katari.entry.interactions.anime.media.immersive.AnimeClipPlatform
import app.katari.entry.interactions.anime.media.immersive.AnimeImmersiveClips
import app.katari.entry.interactions.anime.media.immersive.AnimeImmersiveProcessor
import app.katari.entry.interactions.anime.media.immersive.TouchAnimeClipPlatform
import app.katari.entry.interactions.anime.navigation.AnimeOpenProcessor
import app.katari.entry.interactions.media.EntryMediaCacheProvider
import app.katari.entry.interactions.navigation.EntryOpenProcessor
import app.katari.entry.interactions.runtime.EntryImmersiveProcessor
import app.katari.entry.interactions.runtime.EntryPlayerCache
import app.katari.entry.interactions.runtime.EntryPreviewConfigurationProvider
import app.katari.entry.interactions.runtime.EntryPreviewProcessor
import app.katari.entry.interactions.runtime.EntryTypeKey
import app.katari.entry.interactions.settings.EntryInteractionPreferences
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoMap
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.source.entry.EntryType

/**
 * Anime's contributions that need the Android player: opening, previews, the immersive feed's clips with their touch
 * gestures, and the playback cache. Downloads are in [AnimeDownloadBindings], the player's shared parts in
 * [AnimePlayerBindings], the clips' in [AnimeImmersiveBindings] and the rest in [AnimeEntryTypeBindings], which every
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
    internal fun clipPlatform(): AnimeClipPlatform = TouchAnimeClipPlatform

    @Provides
    @IntoMap
    @EntryTypeKey(EntryType.ANIME)
    internal fun immersive(clips: AnimeImmersiveClips): EntryImmersiveProcessor = AnimeImmersiveProcessor(clips)

    @Provides
    @IntoMap
    @EntryTypeKey(EntryType.ANIME)
    internal fun mediaCache(playerCache: () -> EntryPlayerCache): EntryMediaCacheProvider =
        AnimeMediaCacheProvider(playerCache)
}
