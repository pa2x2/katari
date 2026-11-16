package eu.kanade.tachiyomi.ui.video.player

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import app.katari.presentation.kit.theme.PageOverlayColors

/** [dim] is the share of the picture's light a dark layer takes away, 0 for none; [color] is an ARGB filter. */
@Composable
internal fun VideoPlayerContentOverlay(
    dim: Float,
    color: Int?,
    colorBlendMode: BlendMode?,
    modifier: Modifier = Modifier,
) {
    if (dim > 0f) {
        val layer = remember(dim) { PageOverlayColors.dim(dim) }
        Canvas(modifier = modifier.fillMaxSize()) {
            drawRect(layer)
        }
    }

    if (color != null) {
        Canvas(
            modifier = modifier.fillMaxSize(),
        ) {
            drawRect(
                color = PageOverlayColors.filter(color),
                blendMode = colorBlendMode ?: BlendMode.SrcOver,
            )
        }
    }
}
