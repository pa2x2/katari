package eu.kanade.tachiyomi.ui.video.player

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.network.NetworkHelper
import mihon.entry.interactions.settings.AnimePlayerPreferences
import mihon.entry.viewer.settings.ViewerSettingBinder

/**
 * Graph entry point of the video player activity, which Android constructs. The player's media cache is internal to
 * this module, which a contributed graph interface can't expose, so the graph hands out [AnimePlayerDependencies].
 */
@ContributesTo(AppScope::class)
interface AnimePlayerGraph {
    val animePlayerDependencies: AnimePlayerDependencies
}

@Inject
class AnimePlayerDependencies internal constructor(
    internal val networkHelper: NetworkHelper,
    internal val mediaCache: VideoPlayerMediaCache,
    internal val animePlayerPreferences: AnimePlayerPreferences,
    internal val viewerSettingBinder: ViewerSettingBinder,
)
