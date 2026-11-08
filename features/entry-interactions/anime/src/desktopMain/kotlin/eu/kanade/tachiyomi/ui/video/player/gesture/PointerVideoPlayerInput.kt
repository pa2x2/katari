package eu.kanade.tachiyomi.ui.video.player.gesture

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import eu.kanade.tachiyomi.ui.video.player.picture.VideoPictureGeometry
import eu.kanade.tachiyomi.ui.video.player.surface.VideoSurface
import mihon.video.engine.spi.player.VideoPlayer

/**
 * The desktop's mouse, touchpad and keys: the mouse's buttons ([videoPlayerMouse]), scrolling ([videoPlayerWheel]) and
 * touchpad pinches ([videoPlayerTouchpadPinch]) on the picture, and the player's keys ([VideoPlayerKeys]), with the
 * pointer they move ([VideoPlayerPointer]).
 */
internal object PointerVideoPlayerInput : VideoPlayerInput {
    override val hovers = true

    @Composable
    override fun Picture(player: VideoPlayer, target: VideoPlayerInputTarget, modifier: Modifier) {
        var size by remember { mutableStateOf(IntSize.Zero) }
        val latestSize by rememberUpdatedState(size)
        val geometry = remember(target) {
            {
                target.videoAspectRatio
                    ?.takeIf { latestSize.width > 0 && latestSize.height > 0 }
                    ?.let { VideoPictureGeometry(latestSize.width.toFloat(), latestSize.height.toFloat(), it) }
            }
        }
        val pointer = rememberVideoPlayerPointer()
        VideoPlayerKeys(target = target, seekStepSeconds = target.seekStepSeconds, geometry = geometry)
        VideoSurface(
            player = player,
            framing = { target.pictureFraming },
            aspectRatio = { target.videoAspectRatio },
            modifier = modifier
                .onSizeChanged { size = it }
                .pointerHoverIcon(pointer.icon(target, geometry()))
                .videoPlayerMouse(target, geometry, pointer)
                .videoPlayerWheel(target, geometry, pointer)
                .videoPlayerTouchpadPinch(target, geometry, pointer),
        )
    }

    @Composable
    override fun Guide(seekStepSeconds: Int, onDismiss: () -> Unit, modifier: Modifier) {
        DesktopVideoPlayerGuide(seekStepSeconds = seekStepSeconds, onDismiss = onDismiss, modifier = modifier)
    }
}

internal const val PICTURE_ZOOM_STEP = 1.1f
