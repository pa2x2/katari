package eu.kanade.tachiyomi.ui.video.player.screen

import android.view.View
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import eu.kanade.tachiyomi.ui.video.player.gesture.VideoPlayerPinchGesture
import eu.kanade.tachiyomi.ui.video.player.gesture.VideoPlayerTouchTarget
import eu.kanade.tachiyomi.ui.video.player.gesture.setVideoPlayerTouchGestures
import eu.kanade.tachiyomi.ui.video.player.picture.VideoPicturePlacer
import mihon.presentation.kit.surface.SurfaceContainer
import mihon.presentation.kit.surface.fillArgb
import mihon.presentation.kit.theme.KatariTheme
import mihon.video.engine.spi.media3.media3Player
import mihon.video.engine.spi.player.VideoPlayer

/**
 * The video: a Media3 player view without its own controller or subtitles, the picture framed as [touchTarget] shows
 * it, and the player's touch gestures.
 *
 * The framing follows [touchTarget] outside composition, so a pinch's every move reaches the view without composing it
 * again.
 */
@Composable
@OptIn(markerClass = [UnstableApi::class])
internal fun VideoPlayerSurface(
    player: VideoPlayer,
    touchTarget: VideoPlayerTouchTarget,
    modifier: Modifier = Modifier,
) {
    val mediaArgb = SurfaceContainer.Media.fillArgb()
    val sideGestureEdgeExclusionPx = with(LocalDensity.current) { KatariTheme.sizes.touchTarget.toPx() }
    val placer = remember { VideoPicturePlacer() }
    val pinchGesture = remember(touchTarget) { VideoPlayerPinchGesture(touchTarget) }
    LaunchedEffect(placer, touchTarget, player) {
        snapshotFlow { touchTarget.pictureFraming to touchTarget.videoAspectRatio }
            .collect { (framing, aspectRatio) -> placer.show(framing, aspectRatio, player) }
    }
    AndroidView(
        modifier = modifier,
        factory = { androidContext ->
            PlayerView(androidContext).apply {
                placer.attach(this)
                this.player = player.media3Player
                setKeepContentOnPlayerReset(true)
                useController = false
                setEnableComposeSurfaceSyncWorkaround(true)
                // The app draws the subtitles itself
                subtitleView?.visibility = View.GONE
                setShutterBackgroundColor(mediaArgb)
                setBackgroundColor(mediaArgb)
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
                isClickable = true
                isFocusable = true
            }
        },
        update = { playerView ->
            playerView.player = player.media3Player
            playerView.setKeepContentOnPlayerReset(true)
            playerView.useController = false
            playerView.setVideoPlayerTouchGestures(
                target = touchTarget,
                pinchGesture = pinchGesture,
                edgeExclusionPx = sideGestureEdgeExclusionPx,
            )
        },
    )
}
