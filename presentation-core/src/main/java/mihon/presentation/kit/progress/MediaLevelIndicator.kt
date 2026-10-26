package mihon.presentation.kit.progress

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import mihon.presentation.kit.theme.KatariTheme

/**
 * A vertical meter laid over video or pages in the media colours, for a level a gesture is changing, such as volume
 * or brightness. [level] is 0..1, filled from the bottom. With [boostFrom], the part of the meter above it is a range
 * past the normal full level, such as volume boosted past the device's: a mark across the meter shows where it starts,
 * and the fill above it takes the warning colour.
 */
@Composable
fun MediaLevelIndicator(
    level: Float,
    modifier: Modifier = Modifier,
    boostFrom: Float? = null,
) {
    val colors = KatariTheme.colors
    val track = colors.onMedia.copy(alpha = TRACK_ALPHA)
    val fill = colors.onMedia
    val boostFill = colors.warning
    Canvas(modifier = modifier.size(width = MediaLevelIndicatorWidth, height = MediaLevelIndicatorHeight)) {
        val radius = CornerRadius(size.width / 2f)
        val filled = level.coerceIn(0f, 1f)
        drawRoundRect(track, cornerRadius = radius)
        drawRoundRect(
            color = fill,
            topLeft = Offset(0f, size.height * (1f - filled)),
            size = Size(size.width, size.height * filled),
            cornerRadius = radius,
        )
        val boostStart = boostFrom?.coerceIn(0f, 1f) ?: return@Canvas
        if (filled > boostStart) {
            drawRoundRect(
                color = boostFill,
                topLeft = Offset(0f, size.height * (1f - filled)),
                size = Size(size.width, size.height * (filled - boostStart)),
                cornerRadius = radius,
            )
        }
        val markReach = MarkOverhang.toPx()
        val markY = size.height * (1f - boostStart)
        drawLine(
            color = fill,
            start = Offset(-markReach, markY),
            end = Offset(size.width + markReach, markY),
            strokeWidth = MarkThickness.toPx(),
        )
    }
}

private val MediaLevelIndicatorWidth = 6.dp

private val MediaLevelIndicatorHeight = 120.dp

private val MarkOverhang = 4.dp

private val MarkThickness = 2.dp

private const val TRACK_ALPHA = 0.28f
