package eu.kanade.tachiyomi.ui.video.player.pip

import androidx.compose.runtime.Composable
import eu.kanade.tachiyomi.ui.video.player.playback.VideoPlayerPlaybackSnapshot

/**
 * Picture-in-picture: the video alone in a small window that stays over the others, with previous episode, play/pause
 * and next episode controls of its own. Android has the system's, which shows the screen itself shrunk; desktop's is a
 * window of the player's own (D009), which shows what the screen hands it ([drawsOwnWindow]). The screen hands it the
 * playback state and the episode navigation as they change.
 */
internal interface VideoPlayerPictureInPicture {
    /** Whether the player shows in the small window now; the screen hides its controls and overlays there. */
    val isInPictureInPictureMode: Boolean

    /** Whether the controls offer a button that enters picture-in-picture. */
    val showsButton: Boolean

    /**
     * Whether the small window is the player's own, which draws the [VideoPlayerPictureInPictureScene] the screen
     * composes in [Window] instead of the screen. It has room for the end screen, so an episode's end there shows it
     * small (D019) rather than playing the next episode straight away, as Android's system window does.
     */
    val drawsOwnWindow: Boolean

    /** The playback state the window's controls were last built from. */
    var latestSnapshot: VideoPlayerPlaybackSnapshot

    fun onPlaybackSnapshot(snapshot: VideoPlayerPlaybackSnapshot)

    /** Where the window's previous and next controls go; null greys one out while there's no such episode. */
    fun setEpisodeNavigation(onPreviousEpisode: (() -> Unit)?, onNextEpisode: (() -> Unit)?)

    fun enterFromControls()

    /** Shows [scene] in the window of the player's own while in picture-in-picture, where [drawsOwnWindow]. */
    @Composable
    fun Window(scene: VideoPlayerPictureInPictureScene)
}
