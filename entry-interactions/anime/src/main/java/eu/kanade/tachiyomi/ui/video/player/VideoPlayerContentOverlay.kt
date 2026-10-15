package eu.kanade.tachiyomi.ui.video.player

import androidx.annotation.ColorInt
import androidx.annotation.IntRange
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import mihon.presentation.kit.theme.PageOverlayColors
import kotlin.math.abs

@Composable
internal fun VideoPlayerContentOverlay(
    @IntRange(from = -100, to = 100) brightness: Int,
    @ColorInt color: Int?,
    colorBlendMode: BlendMode?,
    modifier: Modifier = Modifier,
) {
    if (brightness < 0) {
        val dim = remember(brightness) { PageOverlayColors.dim(abs(brightness) / 100f) }
        Canvas(modifier = modifier.fillMaxSize()) {
            drawRect(dim)
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
