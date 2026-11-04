package eu.kanade.tachiyomi.ui.video.player.gesture

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import eu.kanade.tachiyomi.ui.video.player.components.VideoPlayerGestureGuide
import eu.kanade.tachiyomi.ui.video.player.surface.VideoSurface
import mihon.presentation.kit.theme.KatariTheme
import mihon.video.engine.spi.player.VideoPlayer

/** Android's touch gestures, set on the video's own view so they reach it before anything drawn over it. */
internal object TouchVideoPlayerInput : VideoPlayerInput {
    @Composable
    override fun Picture(player: VideoPlayer, target: VideoPlayerInputTarget, modifier: Modifier) {
        val sideGestureEdgeExclusionPx = with(LocalDensity.current) { KatariTheme.sizes.touchTarget.toPx() }
        val pinchGesture = remember(target) { VideoPlayerPinchGesture(target) }
        VideoSurface(
            player = player,
            framing = { target.pictureFraming },
            aspectRatio = { target.videoAspectRatio },
            modifier = modifier,
            configure = { view ->
                view.isClickable = true
                view.isFocusable = true
                view.setVideoPlayerTouchGestures(
                    target = target,
                    pinchGesture = pinchGesture,
                    edgeExclusionPx = sideGestureEdgeExclusionPx,
                )
            },
        )
    }

    @Composable
    override fun Guide(seekStepSeconds: Int, onDismiss: () -> Unit, modifier: Modifier) {
        VideoPlayerGestureGuide(seekStepSeconds = seekStepSeconds, onDismiss = onDismiss, modifier = modifier)
    }
}
