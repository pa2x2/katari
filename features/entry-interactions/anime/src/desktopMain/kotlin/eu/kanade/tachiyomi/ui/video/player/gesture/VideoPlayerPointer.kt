package eu.kanade.tachiyomi.ui.video.player.gesture

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.input.pointer.PointerIcon
import eu.kanade.tachiyomi.ui.video.player.picture.VideoPictureGeometry
import eu.kanade.tachiyomi.ui.video.player.screen.CONTROLS_AUTO_HIDE_DELAY_MS
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import mihon.presentation.kit.pointer.HiddenPointer

/**
 * The mouse pointer over the picture. It shows while the controls do, and for as long as they would after it moves or
 * zooms the picture, so a picture zoomed past the player can be aimed at and dragged with the controls hidden. Over
 * such a picture it is a hand, as the picture can be dragged.
 */
@Stable
internal class VideoPlayerPointer {
    private var moves by mutableLongStateOf(0L)
    private var resting by mutableStateOf(true)

    fun moved() {
        moves += 1
    }

    fun icon(target: VideoPlayerInputTarget, geometry: VideoPictureGeometry?): PointerIcon = when {
        !target.controlsVisible && resting -> HiddenPointer
        geometry?.overflows(target.pictureFraming) == true -> PointerIcon.Hand
        else -> PointerIcon.Default
    }

    /**
     * Whether a move to [y] over [geometry]'s picture shows the controls: anywhere while the picture fits in the player,
     * and only where the bars show while it is zoomed past it. Until the bars have been laid out, anywhere.
     */
    fun showsControlsAt(y: Float, target: VideoPlayerInputTarget, geometry: VideoPictureGeometry?): Boolean {
        val reach = target.barReach ?: return true
        if (geometry == null || !geometry.overflows(target.pictureFraming)) return true
        return reach.covers(y, geometry.height)
    }

    suspend fun restAfterMoves() {
        snapshotFlow { moves }.drop(1).collectLatest {
            resting = false
            delay(CONTROLS_AUTO_HIDE_DELAY_MS)
            resting = true
        }
    }
}

@Composable
internal fun rememberVideoPlayerPointer(): VideoPlayerPointer {
    val pointer = remember { VideoPlayerPointer() }
    LaunchedEffect(pointer) { pointer.restAfterMoves() }
    return pointer
}
