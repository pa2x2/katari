package eu.kanade.tachiyomi.ui.video.player.surface

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import eu.kanade.tachiyomi.ui.video.player.picture.VideoPicturePlacer
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import mihon.presentation.kit.surface.SurfaceContainer
import mihon.presentation.kit.surface.fillArgb
import mihon.video.engine.spi.player.SurfaceViewOutput
import mihon.video.engine.spi.player.VideoPlayer
import mihon.video.engine.spi.player.VideoPlayerEvent
import mihon.video.engine.spi.player.VideoPlayerListener
import mihon.video.engine.spi.tracks.TrackKind
import mihon.video.engine.spi.tracks.VideoTracks
import tachiyomi.domain.entry.model.PictureFraming

/**
 * Draws [player]'s picture here, whichever engine plays it: the picture of a video of [aspectRatio] placed as [framing]
 * asks, on the media colour. Both are read outside composition, so a pinch's every move reaches the picture without
 * composing it again. [configure] sets up the view, as with touch handling, each time it is composed.
 *
 * The view outlives the players drawn into it, so the next episode's player takes over a surface still showing the last
 * frame of the one before. Until the first player shows a picture, and whenever the stream turns out to have no
 * picture, the media colour covers the surface.
 */
@Composable
internal fun VideoSurface(
    player: VideoPlayer,
    framing: () -> PictureFraming,
    aspectRatio: () -> Float?,
    modifier: Modifier = Modifier,
    configure: (VideoSurfaceView) -> Unit = {},
) {
    val mediaArgb = SurfaceContainer.Media.fillArgb()
    val placer = remember { VideoPicturePlacer() }
    var view by remember { mutableStateOf<VideoSurfaceView?>(null) }
    val latestFraming by rememberUpdatedState(framing)
    val latestAspectRatio by rememberUpdatedState(aspectRatio)

    LaunchedEffect(placer, player) {
        snapshotFlow { latestFraming() to latestAspectRatio() }
            .collect { (framing, aspectRatio) -> placer.show(framing, aspectRatio, player) }
    }
    DisposableEffect(player, view) {
        val surfaceView = view ?: return@DisposableEffect onDispose {}
        player.setVideoOutput(SurfaceViewOutput(surfaceView.surfaceView))
        val listener = VideoPlayerListener { event ->
            if (event == VideoPlayerEvent.FirstFrameRendered) surfaceView.setShutterClosed(false)
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.setVideoOutput(null)
        }
    }
    LaunchedEffect(player, view) {
        val surfaceView = view ?: return@LaunchedEffect
        player.state
            .map { it.tracks.hasNoPicture() }
            .distinctUntilChanged()
            .collect { noPicture -> if (noPicture) surfaceView.setShutterClosed(true) }
    }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            VideoSurfaceView(context).also {
                it.setMediaColor(mediaArgb)
                it.setShutterClosed(true)
                placer.attach(it)
                view = it
            }
        },
        update = configure,
    )
}

/** The stream's tracks are known and none of its pictures is chosen, as for a stream of sound alone. */
private fun VideoTracks.hasNoPicture(): Boolean =
    groups.isNotEmpty() && groups.none { it.kind == TrackKind.Video && it.selected }
