package eu.kanade.tachiyomi.ui.video.player.posture

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import mihon.presentation.kit.window.WindowLayout
import mihon.presentation.kit.window.currentWindowLayout

/**
 * How a half-open fold splits the player. The bent screen's crease is hard to tap and splits the picture, so the player
 * keeps what is tapped off it. A flat or closed device, or a window the fold doesn't cross, gets the usual player with
 * the controls over the picture.
 */
internal sealed interface VideoPlayerPosture {
    data object Flat : VideoPlayerPosture

    /**
     * A horizontal fold, the device standing on its lower half like a laptop: the picture fits above [pictureBottom]
     * and the controls, always shown, take the flat half below [controlsTop].
     */
    data class Tabletop(val pictureBottom: Dp, val controlsTop: Dp) : VideoPlayerPosture

    /** A vertical fold, held like a book: the centre buttons move into the start half, [startHalfWidth] wide. */
    data class Book(val startHalfWidth: Dp) : VideoPlayerPosture
}

@Composable
@ReadOnlyComposable
internal fun currentVideoPlayerPosture(): VideoPlayerPosture =
    videoPlayerPosture(currentWindowLayout(), LocalLayoutDirection.current)

internal fun videoPlayerPosture(window: WindowLayout, layoutDirection: LayoutDirection): VideoPlayerPosture {
    val fold = window.fold?.takeIf { it.isHalfOpen } ?: return VideoPlayerPosture.Flat
    if (!fold.isVertical) return VideoPlayerPosture.Tabletop(pictureBottom = fold.start, controlsTop = fold.end)
    // The fold is in the window's left-to-right coordinates; right-to-left layouts start on the right
    val startHalfWidth = if (layoutDirection == LayoutDirection.Ltr) fold.start else window.size.width - fold.end
    return VideoPlayerPosture.Book(startHalfWidth)
}
