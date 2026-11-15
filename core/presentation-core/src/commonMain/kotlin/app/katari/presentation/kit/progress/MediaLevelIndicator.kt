package app.katari.presentation.kit.progress

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import app.katari.presentation.kit.theme.KatariTheme

/**
 * A vertical meter laid over video or pages in the media colours, for a level a gesture is changing, such as volume
 * or brightness. [level] is 0..1, filled from the bottom. [normal] is the part of the meter the level keeps to normally;
 * the parts above and below it are ranges past the normal levels, such as volume boosted past the device's full or a
 * picture dimmed past the screen's lowest brightness. A mark across the meter shows where each such range starts
 * inside the meter, and while the level is in one, the fill in it takes the warning colour.
 */
@Composable
fun MediaLevelIndicator(
    level: Float,
    modifier: Modifier = Modifier,
    normal: ClosedFloatingPointRange<Float> = 0f..1f,
) {
    val colors = KatariTheme.colors
    val track = colors.onMedia.copy(alpha = TRACK_ALPHA)
    val fill = colors.onMedia
    val pastNormalFill = colors.warning
    Canvas(modifier = modifier.size(width = MediaLevelIndicatorWidth, height = MediaLevelIndicatorHeight)) {
        val radius = CornerRadius(size.width / 2f)
        val filled = level.coerceIn(0f, 1f)
        val normalStart = normal.start.coerceIn(0f, 1f)
        val normalEnd = normal.endInclusive.coerceIn(normalStart, 1f)
        drawRoundRect(track, cornerRadius = radius)
        drawFill(fill, from = 0f, to = filled, radius)
        if (filled > normalEnd) drawFill(pastNormalFill, from = normalEnd, to = filled, radius)
        if (filled < normalStart) drawFill(pastNormalFill, from = 0f, to = filled, radius)
        if (normalStart > 0f && normalStart < 1f) drawMark(fill, normalStart)
        if (normalEnd > 0f && normalEnd < 1f) drawMark(fill, normalEnd)
    }
}

private fun DrawScope.drawFill(
    color: Color,
    from: Float,
    to: Float,
    radius: CornerRadius,
) {
    if (to <= from) return
    drawRoundRect(
        color = color,
        topLeft = Offset(0f, size.height * (1f - to)),
        size = Size(size.width, size.height * (to - from)),
        cornerRadius = radius,
    )
}

private fun DrawScope.drawMark(color: Color, at: Float) {
    val markReach = MarkOverhang.toPx()
    val markY = size.height * (1f - at)
    drawLine(
        color = color,
        start = Offset(-markReach, markY),
        end = Offset(size.width + markReach, markY),
        strokeWidth = MarkThickness.toPx(),
    )
}

private val MediaLevelIndicatorWidth = 6.dp

private val MediaLevelIndicatorHeight = 120.dp

private val MarkOverhang = 4.dp

private val MarkThickness = 2.dp

private const val TRACK_ALPHA = 0.28f
