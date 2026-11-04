package eu.kanade.tachiyomi.ui.video.player.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import eu.kanade.tachiyomi.ui.video.player.gesture.VideoPlayerPinchGesture
import eu.kanade.tachiyomi.ui.video.player.gesture.VideoPlayerTouchTarget
import eu.kanade.tachiyomi.ui.video.player.gesture.setVideoPlayerTouchGestures
import eu.kanade.tachiyomi.ui.video.player.surface.VideoSurface
import mihon.presentation.kit.theme.KatariTheme
import mihon.video.engine.spi.player.VideoPlayer

/** The watched video, framed as [touchTarget] shows it, with the player's touch gestures on it. */
@Composable
internal fun VideoPlayerSurface(
    player: VideoPlayer,
    touchTarget: VideoPlayerTouchTarget,
    modifier: Modifier = Modifier,
) {
    val sideGestureEdgeExclusionPx = with(LocalDensity.current) { KatariTheme.sizes.touchTarget.toPx() }
    val pinchGesture = remember(touchTarget) { VideoPlayerPinchGesture(touchTarget) }
    VideoSurface(
        player = player,
        framing = { touchTarget.pictureFraming },
        aspectRatio = { touchTarget.videoAspectRatio },
        modifier = modifier,
        configure = { view ->
            view.isClickable = true
            view.isFocusable = true
            view.setVideoPlayerTouchGestures(
                target = touchTarget,
                pinchGesture = pinchGesture,
                edgeExclusionPx = sideGestureEdgeExclusionPx,
            )
        },
    )
}
