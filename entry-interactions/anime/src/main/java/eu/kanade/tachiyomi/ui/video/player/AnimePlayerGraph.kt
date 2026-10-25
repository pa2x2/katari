package eu.kanade.tachiyomi.ui.video.player

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Inject
import mihon.entry.interactions.settings.AnimePlayerPreferences
import mihon.entry.viewer.settings.ViewerSettingBinder
import mihon.video.engine.mpv.MpvVideoEngine
import mihon.video.engine.mpv.addon.MpvAddon
import mihon.video.engine.mpv.install.MpvAddonInstallation
import mihon.video.engine.spi.VideoEngine

/**
 * Graph entry point of the video player activity, which Android constructs. The player's dependencies are internal to
 * this module, which a contributed graph interface can't expose, so the graph hands out [AnimePlayerDependencies].
 */
@ContributesTo(AppScope::class)
interface AnimePlayerGraph {
    val animePlayerDependencies: AnimePlayerDependencies
}

@Inject
class AnimePlayerDependencies internal constructor(
    internal val videoEngine: VideoEngine,
    internal val mpvEngine: MpvVideoEngine,
    internal val mpvAddon: MpvAddon,
    internal val mpvAddonInstallation: MpvAddonInstallation,
    internal val animePlayerPreferences: AnimePlayerPreferences,
    internal val viewerSettingBinder: ViewerSettingBinder,
)
