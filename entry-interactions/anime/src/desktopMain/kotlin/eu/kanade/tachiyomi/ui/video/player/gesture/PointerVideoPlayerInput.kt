package eu.kanade.tachiyomi.ui.video.player.gesture

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import eu.kanade.tachiyomi.ui.video.player.surface.VideoSurface
import mihon.video.engine.spi.player.VideoPlayer

/**
 * The desktop's picture: moving or pressing the mouse over it brings the controls back, which then hide again on
 * their own while playing. Everything else is done with the controls themselves.
 */
internal object PointerVideoPlayerInput : VideoPlayerInput {
    @Composable
    override fun Picture(player: VideoPlayer, target: VideoPlayerInputTarget, modifier: Modifier) {
        VideoSurface(
            player = player,
            framing = { target.pictureFraming },
            aspectRatio = { target.videoAspectRatio },
            modifier = modifier.pointerInput(target) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        if (event.type == PointerEventType.Move || event.type == PointerEventType.Press) {
                            target.registerControllerInteraction(showControls = true)
                        }
                    }
                }
            },
        )
    }

    // The desktop has no gestures to explain yet; the visit never asks for the guide.
    @Composable
    override fun Guide(seekStepSeconds: Int, onDismiss: () -> Unit, modifier: Modifier) = Unit
}
