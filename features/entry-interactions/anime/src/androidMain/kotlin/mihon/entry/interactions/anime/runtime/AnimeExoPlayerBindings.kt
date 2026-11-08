package mihon.entry.interactions.anime.runtime

import android.app.Application
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.ui.video.player.engine.exoplayer.ExoPlayerMediaCache
import eu.kanade.tachiyomi.ui.video.player.engine.exoplayer.ExoPlayerVideoEngine
import mihon.entry.interactions.runtime.EntryPlayerCache
import mihon.video.engine.spi.VideoEngine

/** Android's built-in engine, ExoPlayer, and the playback cache it fills. */
@ContributesTo(AppScope::class)
@BindingContainer
object AnimeExoPlayerBindings {
    @Provides
    @SingleIn(AppScope::class)
    internal fun mediaCache(application: Application): ExoPlayerMediaCache = ExoPlayerMediaCache(application)

    @Provides
    internal fun playerCache(cache: ExoPlayerMediaCache): EntryPlayerCache = cache

    @Provides
    @SingleIn(AppScope::class)
    internal fun videoEngine(
        application: Application,
        networkHelper: NetworkHelper,
        mediaCache: ExoPlayerMediaCache,
    ): VideoEngine = ExoPlayerVideoEngine(application, networkHelper, mediaCache)
}
