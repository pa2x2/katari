package eu.kanade.tachiyomi.ui.video.player.screen

import eu.kanade.tachiyomi.ui.video.player.VideoPlayerOrientationRequests
import eu.kanade.tachiyomi.ui.video.player.brightness.VideoPlayerBrightness
import eu.kanade.tachiyomi.ui.video.player.engine.VideoPlayerEngines
import eu.kanade.tachiyomi.ui.video.player.gesture.VideoPlayerInput
import eu.kanade.tachiyomi.ui.video.player.mediacontrols.VideoPlayerMediaControls
import eu.kanade.tachiyomi.ui.video.player.pip.VideoPlayerPictureInPicture
import eu.kanade.tachiyomi.ui.video.player.playback.VideoPlayerPlaybackController
import eu.kanade.tachiyomi.ui.video.player.recovery.VideoProblemAction
import eu.kanade.tachiyomi.ui.video.player.surface.SeekPreviewSurface
import eu.kanade.tachiyomi.ui.video.player.volume.VideoPlayerVolume
import mihon.core.platform.clipboard.TextClipboard
import mihon.entry.interactions.settings.AnimePlayerPreferences
import mihon.entry.interactions.source.EntryChildWebViewAction
import mihon.entry.interactions.source.EntryChildWebViewResolution

/**
 * What the player screen gets from the platform that hosts it: the owners that outlive each episode's composition, the
 * visit's state and settings, what the platform does its own way, and the actions that reach outside the screen.
 */
internal class VideoPlayerScreenHost(
    val engines: VideoPlayerEngines,
    val preferences: AnimePlayerPreferences,
    val settings: VideoPlayerSettingBindings,
    val visit: VideoPlayerVisitState,
    val playback: VideoPlayerPlaybackController,
    val input: VideoPlayerInput,
    val seekPreviewSurface: SeekPreviewSurface,
    val pictureInPicture: VideoPlayerPictureInPicture,
    val mediaControls: VideoPlayerMediaControls,
    val volume: VideoPlayerVolume,
    /** Null where the player can't change the screen's brightness, as on desktop. */
    val brightness: VideoPlayerBrightness?,
    /** Null where the window doesn't turn, as on desktop, which leaves out the rotate button. */
    val orientation: VideoPlayerOrientationRequests?,
    /** Whether the controls can be locked against stray touches; desktop has nothing to lock them against (D008). */
    val locksControls: Boolean,
    val clipboard: TextClipboard,
    val onClose: () -> Unit,
    /** Hides the system bars again where the platform shows them over the player. */
    val onHideSystemUi: () -> Unit,
    /** The player's own settings screen, from the settings sheet's menu. */
    val onOpenDefaultSettings: () -> Unit,
    /** Leaves the player for the screen that fixes why the episode couldn't open. */
    val onProblemAction: (VideoProblemAction) -> Unit,
    val onOpenWebView: (
        action: EntryChildWebViewAction,
        resolution: EntryChildWebViewResolution.Available,
        title: String,
    ) -> Unit,
)
