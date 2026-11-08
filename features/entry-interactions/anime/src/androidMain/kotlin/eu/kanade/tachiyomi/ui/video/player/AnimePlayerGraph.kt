package eu.kanade.tachiyomi.ui.video.player

import app.katari.core.platform.clipboard.TextClipboard
import app.katari.entry.interactions.settings.AnimePlayerDevicePreferences
import app.katari.entry.interactions.settings.AnimePlayerPreferences
import app.katari.entry.viewer.settings.ViewerSettingBinder
import app.katari.video.engine.mpv.MpvVideoEngine
import app.katari.video.engine.mpv.addon.MpvAddon
import app.katari.video.engine.mpv.install.MpvAddonInstallation
import app.katari.video.engine.spi.VideoEngine
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Inject

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
    internal val devicePreferences: AnimePlayerDevicePreferences,
    internal val viewerSettingBinder: ViewerSettingBinder,
    internal val textClipboard: TextClipboard,
)
