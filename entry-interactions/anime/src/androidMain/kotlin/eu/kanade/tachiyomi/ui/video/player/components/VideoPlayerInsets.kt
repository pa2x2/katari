package eu.kanade.tachiyomi.ui.video.player.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp

internal data class VideoPlayerSafeInsets(
    val horizontal: Int,
    val top: Int,
    val bottom: Int,
)

internal fun resolveVideoPlayerSafeInsets(
    left: Int,
    top: Int,
    right: Int,
    bottom: Int,
): VideoPlayerSafeInsets {
    return VideoPlayerSafeInsets(
        horizontal = maxOf(left, right).coerceAtLeast(0),
        top = top.coerceAtLeast(0),
        bottom = bottom.coerceAtLeast(0),
    )
}

@Composable
internal fun Modifier.videoPlayerSafeContentPadding(
    includeTop: Boolean = false,
    includeBottom: Boolean = false,
): Modifier {
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val safeDrawing = WindowInsets.safeDrawing
    val insets = resolveVideoPlayerSafeInsets(
        left = safeDrawing.getLeft(density, layoutDirection),
        top = safeDrawing.getTop(density),
        right = safeDrawing.getRight(density, layoutDirection),
        bottom = safeDrawing.getBottom(density),
    )

    val modifier = this
    return with(density) {
        val horizontal = insets.horizontal.toDp()
        modifier.padding(
            start = horizontal,
            top = if (includeTop) insets.top.toDp() else 0.dp,
            end = horizontal,
            bottom = if (includeBottom) insets.bottom.toDp() else 0.dp,
        )
    }
}
