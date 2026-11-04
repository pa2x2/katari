package eu.kanade.tachiyomi.ui.video.player.window

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import eu.kanade.tachiyomi.ui.video.player.playback.VideoPlayerPlaybackController
import mihon.core.platform.power.ScreenIdleInhibitor
import mihon.video.engine.spi.player.VideoPlayerState

/**
 * Keeps the screen awake while the watched episode plays, or waits to play, where it is seen: in the main window while
 * that is shown, or in picture-in-picture ([inPictureInPicture]), whatever the main window does.
 */
@Composable
internal fun VideoPlayerKeepAwake(
    playback: VideoPlayerPlaybackController,
    inhibitor: ScreenIdleInhibitor,
    inPictureInPicture: Boolean,
) {
    val player = playback.player
    val playing by produceState(false, player) {
        value = false
        player?.state?.collect { value = it.meansToPlay }
    }
    KeepScreenAwake(playing = playing, inhibitor = inhibitor, evenWhenHidden = inPictureInPicture)
}

// Buffering counts, so a stall doesn't let go of the screen only to take it again a moment later
private val VideoPlayerState.meansToPlay: Boolean
    get() = playWhenReady && when (playback) {
        VideoPlayerState.Playback.Ready, VideoPlayerState.Playback.Buffering -> true
        VideoPlayerState.Playback.Idle, VideoPlayerState.Playback.Ended -> false
    }
