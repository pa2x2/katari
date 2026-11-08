package app.katari.entry.interactions.anime.runtime

import app.katari.entry.interactions.anime.media.immersive.AnimeClipPlatform
import app.katari.entry.interactions.anime.media.immersive.AnimeImmersiveClips
import app.katari.entry.interactions.anime.media.session.AnimeMediaSessionProcessor
import app.katari.video.engine.spi.VideoEngine
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.ui.video.player.AnimePlayerBasePreferences
import eu.kanade.tachiyomi.ui.video.player.VideoStreamResolver
import tachiyomi.domain.entry.repository.EntryProgressRepository

/**
 * Anime's clips in the immersive feed, alike on every platform: each plays them in its engine, binds the
 * [AnimeClipPlatform] that takes the viewer's input and keeps the screen awake, and answers the feed's immersive
 * processor, a platform facility, with these.
 */
@ContributesTo(AppScope::class)
@BindingContainer
object AnimeImmersiveBindings {
    @Provides
    @SingleIn(AppScope::class)
    internal fun clips(
        entryProgressRepository: EntryProgressRepository,
        resolveVideoStream: () -> VideoStreamResolver,
        mediaSession: AnimeMediaSessionProcessor,
        videoEngine: () -> VideoEngine,
        basePreferences: () -> AnimePlayerBasePreferences,
        clipPlatform: AnimeClipPlatform,
    ): AnimeImmersiveClips = AnimeImmersiveClips(
        entryProgressRepository = entryProgressRepository,
        resolveVideoStream = resolveVideoStream,
        mediaSession = mediaSession,
        videoEngine = videoEngine,
        basePreferences = basePreferences,
        clipPlatform = clipPlatform,
    )
}
