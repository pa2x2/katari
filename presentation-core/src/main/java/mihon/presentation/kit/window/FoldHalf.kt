package mihon.presentation.kit.window

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** One side of a vertical fold, as its left and right edges in the window, whatever the layout direction. */
@Immutable
internal data class FoldHalf(val left: Dp, val right: Dp) {
    val width: Dp get() = right - left
}

/**
 * The half of the window a dialog or sheet opens in while a vertical fold is half-open, since content across the bend
 * is hard to read: the half last touched, where the user is acting, or the right half before any touch. Null when the
 * window has no half-open vertical fold, and the surface uses the whole window. Taken once as the surface opens and
 * again only if the window changes, so it doesn't jump while open.
 */
@Composable
internal fun rememberFoldHalf(): FoldHalf? {
    val layout = currentWindowLayout()
    val lastTouch = LocalLastTouch.current
    val density = LocalDensity.current
    return remember(layout, density) {
        val fold = layout.fold?.takeIf { it.isVertical && it.isHalfOpen } ?: return@remember null
        val touchX = lastTouch?.positionInWindow?.let { with(density) { it.x.toDp() } }
        if (touchX != null && touchX < fold.start) {
            FoldHalf(left = 0.dp, right = fold.start)
        } else {
            FoldHalf(left = fold.end, right = layout.size.width)
        }
    }
}
