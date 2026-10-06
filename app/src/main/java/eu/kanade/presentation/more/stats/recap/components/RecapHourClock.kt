package eu.kanade.presentation.more.stats.recap.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.more.stats.recap.motion.LocalRecapClock
import eu.kanade.presentation.more.stats.recap.motion.RECAP_GROW_MILLIS
import eu.kanade.presentation.more.stats.recap.motion.ambientWave
import eu.kanade.presentation.more.stats.recap.motion.rememberRecapEntrance
import kotlin.math.cos
import kotlin.math.sin

/**
 * A day as a clock face: one wedge per hour, growing outward with the time spent in it, midnight at the top. The
 * busiest hours are drawn in full accent, and [topHour] slowly brightens and dims. The wedges grow out at step [order]
 * of the page's entrance.
 *
 * @param hourlyDurationMillis 24 values from midnight.
 */
@Composable
internal fun RecapHourClock(
    hourlyDurationMillis: List<Long>,
    topHour: Int,
    centerLabel: String,
    hourLabel: (Int) -> String,
    size: Dp,
    order: Int,
    modifier: Modifier = Modifier,
) {
    val palette = LocalRecapPalette.current
    val grow = rememberRecapEntrance(order, RECAP_GROW_MILLIS)
    val clock = LocalRecapClock.current
    val measurer = rememberTextMeasurer()
    val max = hourlyDurationMillis.max().coerceAtLeast(1L).toFloat()
    Canvas(modifier.size(size)) {
        val center = this.center
        val outer = this.size.minDimension / 2f - LABEL_SPACE.toPx()
        val inner = outer * INNER_RATIO
        hourlyDurationMillis.forEachIndexed { hour, duration ->
            if (duration <= 0L) return@forEachIndexed
            val share = duration / max
            val radius = inner + (outer - inner) * maxOf(MIN_SHARE, share) * grow.value
            val start = hour * WEDGE_DEGREES - 90f + GAP_DEGREES / 2f
            val sweep = WEDGE_DEGREES - GAP_DEGREES
            val path = Path().apply {
                arcTo(circleBounds(center, radius), start, sweep, forceMoveTo = true)
                arcTo(circleBounds(center, inner), start + sweep, -sweep, forceMoveTo = false)
                close()
            }
            val color = when {
                hour == topHour -> lerp(
                    palette.accent,
                    palette.ink,
                    TOP_BRIGHTEN * ambientWave(clock.storyMillis, TOP_PULSE_MILLIS),
                )
                share >= BUSY_SHARE -> palette.accent
                else -> palette.accent.copy(alpha = 0.45f)
            }
            drawPath(path, color = color)
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

/** The least a wedge shows, so a little time still reads as some; no time shows no wedge. */
private const val MIN_SHARE = 0.06f
private const val BUSY_SHARE = 0.8f
private const val TOP_BRIGHTEN = 0.6f
private const val TOP_PULSE_MILLIS = 2_400L
