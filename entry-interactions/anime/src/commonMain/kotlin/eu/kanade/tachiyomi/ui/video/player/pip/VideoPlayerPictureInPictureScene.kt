package eu.kanade.tachiyomi.ui.video.player.pip

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import eu.kanade.tachiyomi.ui.video.player.gesture.VideoPlayerInputTarget
import eu.kanade.tachiyomi.ui.video.player.playback.VideoPlayerPlaybackSnapshot
import mihon.video.engine.spi.player.VideoPlayer

/**
 * What the watch screen hands a picture-in-picture window of the player's own: the episode's [player], whose picture
 * the window draws fitted, the [input] its buttons and keys act through, and what the screen draws over the picture
 * there: the [subtitles], sized for the window, and the [endScreen], small, while [endScreenVisible]. [title] names
 * the series and episode.
 */
internal class VideoPlayerPictureInPictureScene(
    val player: VideoPlayer,
    val input: VideoPlayerInputTarget,
    val snapshot: VideoPlayerPlaybackSnapshot,
    val title: String,
    val endScreenVisible: Boolean,
    val subtitles: @Composable (Modifier) -> Unit,
    val endScreen: @Composable (Modifier) -> Unit,
)
