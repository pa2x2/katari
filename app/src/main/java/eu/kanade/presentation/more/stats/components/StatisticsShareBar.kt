package eu.kanade.presentation.more.stats.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import mihon.presentation.kit.content.StatisticsMetrics
import mihon.presentation.kit.theme.KatariTheme

/** A rounded bar filled to [fraction] of its width in the series [color], over the empty track. */
@Composable
internal fun StatisticsShareBar(
    fraction: Float,
    color: Color,
    modifier: Modifier = Modifier,
    height: Dp = StatisticsMetrics.shareBar,
) {
    val track = KatariTheme.colors.surfaceVariant
    Canvas(modifier.fillMaxWidth().height(height).clip(KatariTheme.shapes.full)) {
        drawRect(track)
        val filled = size.width * fraction.coerceIn(0f, 1f)
        if (filled > 0f) {
            drawRoundRect(color, size = Size(filled, size.height), cornerRadius = CornerRadius(size.height / 2f))
        }
    }
}
