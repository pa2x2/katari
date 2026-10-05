package eu.kanade.presentation.more.stats.recap.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/**
 * A day as a clock face: one wedge per hour, growing outward with the time spent in it, midnight at the top. The
 * busiest hours are drawn in full accent.
 *
 * @param hourlyDurationMillis 24 values from midnight.
 */
@Composable
internal fun RecapHourClock(
    hourlyDurationMillis: List<Long>,
    centerLabel: String,
    hourLabel: (Int) -> String,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    val palette = LocalRecapPalette.current
    val reveal = LocalRecapReveal.current
    val measurer = rememberTextMeasurer()
    val max = hourlyDurationMillis.max().coerceAtLeast(1L).toFloat()
    Canvas(modifier.size(size)) {
        val grow = FastOutSlowInEasing.transform(reveal.value.coerceIn(0f, 1f))
        val center = this.center
        val outer = this.size.minDimension / 2f - LABEL_SPACE.toPx()
        val inner = outer * INNER_RATIO
        hourlyDurationMillis.forEachIndexed { hour, duration ->
            val share = duration / max
            val radius = inner + (outer - inner) * maxOf(MIN_SHARE, share) * grow
            val start = hour * WEDGE_DEGREES - 90f + GAP_DEGREES / 2f
            val sweep = WEDGE_DEGREES - GAP_DEGREES
            val path = Path().apply {
                arcTo(circleBounds(center, radius), start, sweep, forceMoveTo = true)
                arcTo(circleBounds(center, inner), start + sweep, -sweep, forceMoveTo = false)
                close()
            }
            drawPath(path, color = if (share >= BUSY_SHARE) palette.accent else palette.accent.copy(alpha = 0.45f))
        }
        listOf(0, 6, 12, 18).forEach { hour ->
            val angle = Math.toRadians(hour * WEDGE_DEGREES - 90.0)
            val layout = measurer.measure(hourLabel(hour), RecapTypography.Tiny.copy(color = palette.muted))
            val distance = outer + LABEL_SPACE.toPx() / 2f
            drawText(
                layout,
                topLeft = Offset(
                    center.x + (distance * cos(angle)).toFloat() - layout.size.width / 2f,
                    center.y + (distance * sin(angle)).toFloat() - layout.size.height / 2f,
                ),
            )
        }
        val label = measurer.measure(centerLabel, RecapTypography.Title.copy(color = palette.ink))
        drawText(label, topLeft = center - Offset(label.size.width / 2f, label.size.height / 2f))
    }
}

private fun circleBounds(center: Offset, radius: Float) = Rect(
    center - Offset(radius, radius),
    Size(
        radius * 2,
        radius * 2,
    ),
)

private val LABEL_SPACE = 22.dp
private const val INNER_RATIO = 0.36f
private const val WEDGE_DEGREES = 15f
private const val GAP_DEGREES = 3f
private const val MIN_SHARE = 0.06f
private const val BUSY_SHARE = 0.8f
