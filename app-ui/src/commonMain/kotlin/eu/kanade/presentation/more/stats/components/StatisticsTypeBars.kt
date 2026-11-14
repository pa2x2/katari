package eu.kanade.presentation.more.stats.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import app.katari.presentation.kit.content.StatisticsMetrics
import app.katari.presentation.kit.content.drawingStyle
import app.katari.presentation.kit.theme.KatariTheme
import app.katari.presentation.kit.theme.TextRole
import eu.kanade.tachiyomi.source.entry.EntryType

/**
 * Evenly spaced bars of time, each stacked from the bottom in the order of [colors]' types, such as each day of a
 * period split by type, with optional labels centred under single bars. [description] replaces the drawing for
 * accessibility services.
 */
@Composable
fun StatisticsTypeBars(
    bars: List<Map<EntryType, Long>>,
    colors: Map<EntryType, Color>,
    labels: List<String?>,
    description: String,
    barAreaHeight: Dp,
    modifier: Modifier = Modifier,
) {
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = TextRole.Badge.drawingStyle
    val labelColor = KatariTheme.chart.neutral
    val trackColor = KatariTheme.chart.track
    val maxValue = bars.maxOfOrNull { it.values.sum() }?.takeIf { it > 0L } ?: 1L
    Canvas(
        modifier
            .fillMaxWidth()
            .height(barAreaHeight + StatisticsMetrics.rhythmLabelArea)
            .semantics { contentDescription = description },
    ) {
        val slot = size.width / bars.size
        val barWidth = minOf(slot * BAR_WIDTH_FRACTION, StatisticsMetrics.barMaxWidth.toPx())
        val barBottom = barAreaHeight.toPx()
        val radius = CornerRadius(StatisticsMetrics.rhythmBarCorner.toPx())
        val minHeight = StatisticsMetrics.rhythmBarMinHeight.toPx()
        bars.forEachIndexed { index, bar ->
            val left = slot * index + (slot - barWidth) / 2f
            drawRoundRect(trackColor, Offset(left, 0f), Size(barWidth, barBottom), radius)
            val total = bar.values.sum()
            if (total <= 0L) return@forEachIndexed
            val height = (barBottom * total / maxValue).coerceAtLeast(minHeight)
            // Each type is drawn from the bottom up to its top, the highest first, so the lower ones cover the rest.
            var top = barBottom - height
            colors.entries.reversed().forEach { (type, color) ->
                val value = bar[type] ?: 0L
                if (value <= 0L) return@forEach
                drawRoundRect(color, Offset(left, top), Size(barWidth, barBottom - top), radius)
                top += height * value / total
            }
        }
        drawSlotLabels(labels, slot, barBottom, textMeasurer, labelStyle, labelColor)
    }
}
