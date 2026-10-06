package eu.kanade.presentation.more.stats.recap.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.more.stats.components.rememberStatisticsDurationFormatter
import eu.kanade.presentation.more.stats.recap.motion.rememberRecapCount
import kotlin.math.ceil

/**
 * [durationMillis] as days laid end to end: a ring for each full 24 hours and a part-filled one for the hours left
 * over, minutes aside. The rings fill one after another as the total counts up at step [order], and shrink to fit
 * when there are many; while they're large, each says how many hours it holds.
 */
@Composable
internal fun RecapDayRings(durationMillis: Long, order: Int, modifier: Modifier = Modifier) {
    val palette = LocalRecapPalette.current
    val formatDuration = rememberStatisticsDurationFormatter()
    val count = rememberRecapCount(order)
    val hours = (durationMillis / HOUR_MILLIS).toInt()
    val rings = List(ceil(hours / HOURS_IN_DAY.toFloat()).toInt()) { minOf(HOURS_IN_DAY, hours - it * HOURS_IN_DAY) }
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val size = ringSize(rings.size, maxWidth)
        val gap = size / 5
        val columns = columnsFor(size, maxWidth)
        Column(verticalArrangement = Arrangement.spacedBy(gap)) {
            rings.withIndex().chunked(columns).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                    row.forEach { (index, ringHours) ->
                        Box(Modifier.size(size), contentAlignment = Alignment.Center) {
                            Canvas(Modifier.fillMaxSize()) {
                                val stroke = (size * STROKE_SHARE).coerceAtLeast(2.dp).toPx()
                                val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
                                val topLeft = Offset(stroke / 2f, stroke / 2f)
                                drawArc(palette.faint, 0f, 360f, false, topLeft, arcSize, style = Stroke(stroke))
                                val shown = (count.value * hours - index * HOURS_IN_DAY)
                                    .coerceIn(0f, ringHours.toFloat())
                                if (shown > 0f) {
                                    drawArc(
                                        color = palette.ink,
                                        startAngle = -90f,
                                        sweepAngle = 360f * shown / HOURS_IN_DAY,
                                        useCenter = false,
                                        topLeft = topLeft,
                                        size = arcSize,
                                        style = Stroke(stroke, cap = StrokeCap.Round),
                                    )
                                }
                            }
                            if (size >= LABELLED_SIZE) {
                                RecapText(formatDuration(ringHours * HOUR_MILLIS), RecapTypography.RowTitle)
                            }
                        }
                    }
                }
            }
        }
    }
}

/** The largest ring, up to [MAX_SIZE], at which [count] rings fit [width] within [MAX_HEIGHT]. */
private fun ringSize(count: Int, width: Dp): Dp {
    var size = MAX_SIZE
    while (size > MIN_SIZE) {
        val rows = ceil(count / columnsFor(size, width).toFloat())
        if ((size + size / 5) * rows - size / 5 <= MAX_HEIGHT) return size
        size -= 2.dp
    }
    return MIN_SIZE
}

private fun columnsFor(size: Dp, width: Dp): Int = ((width + size / 5) / (size + size / 5)).toInt().coerceAtLeast(1)

private const val HOURS_IN_DAY = 24
private const val STROKE_SHARE = 0.12f
private val MAX_SIZE = 76.dp
private val MIN_SIZE = 10.dp
private val MAX_HEIGHT = 190.dp
private val LABELLED_SIZE = 56.dp
