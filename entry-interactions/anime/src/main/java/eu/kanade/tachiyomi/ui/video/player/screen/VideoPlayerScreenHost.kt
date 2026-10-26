package eu.kanade.tachiyomi.ui.video.player.screen

import eu.kanade.tachiyomi.ui.video.player.background.VideoPlayerBackgroundPlayback
import eu.kanade.tachiyomi.ui.video.player.brightness.VideoPlayerBrightness
import eu.kanade.tachiyomi.ui.video.player.pip.VideoPlayerPictureInPicture
import eu.kanade.tachiyomi.ui.video.player.playback.VideoPlayerPlaybackController
import eu.kanade.tachiyomi.ui.video.player.recovery.VideoProblemAction
import eu.kanade.tachiyomi.ui.video.player.volume.VideoPlayerVolume
import mihon.entry.interactions.settings.AnimePlayerPreferences
import mihon.entry.interactions.source.EntryChildWebViewAction
import mihon.entry.interactions.source.EntryChildWebViewResolution
import mihon.video.engine.mpv.MpvVideoEngine
import mihon.video.engine.mpv.addon.MpvAddon
import mihon.video.engine.mpv.install.MpvAddonInstallation
import mihon.video.engine.spi.VideoEngine

/**
 * What the player screen gets from its activity: the owners that outlive each episode's composition, the visit's state
 * and settings, and the actions that reach outside the screen.
 */
internal class VideoPlayerScreenHost(
    val engine: VideoEngine,
    val mpvEngine: MpvVideoEngine,
    val mpvAddon: MpvAddon,
    val mpvAddonInstallation: MpvAddonInstallation,
    val preferences: AnimePlayerPreferences,
    val settings: VideoPlayerSettingBindings,
    val visit: VideoPlayerVisitState,
    val playback: VideoPlayerPlaybackController,
    val pictureInPicture: VideoPlayerPictureInPicture,
    val brightness: VideoPlayerBrightness,
    val volume: VideoPlayerVolume,
    val background: VideoPlayerBackgroundPlayback,
    val onClose: () -> Unit,
    val onHideSystemUi: () -> Unit,
    /** Anime player settings, where the mpv add-on is installed. */
    val onOpenAddonSettings: () -> Unit,
    /** Leaves the player for the screen that fixes why the episode couldn't open. */
    val onProblemAction: (VideoProblemAction) -> Unit,
    val onOpenWebView: (
        action: EntryChildWebViewAction,
        resolution: EntryChildWebViewResolution.Available,
        title: String,
    ) -> Unit,
)
