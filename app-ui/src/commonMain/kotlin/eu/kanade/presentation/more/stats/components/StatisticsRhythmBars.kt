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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import app.katari.presentation.kit.content.StatisticsMetrics
import app.katari.presentation.kit.content.drawingStyle
import app.katari.presentation.kit.theme.KatariTheme
import app.katari.presentation.kit.theme.TextRole

/**
 * Evenly spaced vertical bars with optional labels centered under individual bars. The [highlightIndex] bar
 * uses the full [color]; the rest are muted so the peak stands out. [description] replaces the drawing for
 * accessibility services.
 */
@Composable
fun StatisticsRhythmBars(
    values: List<Long>,
    labels: List<String?>,
    highlightIndex: Int?,
    color: Color,
    description: String,
    barAreaHeight: Dp,
    modifier: Modifier = Modifier,
) {
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = TextRole.Badge.drawingStyle
    val labelColor = KatariTheme.chart.neutral
    val trackColor = KatariTheme.chart.track
    val mutedColor = KatariTheme.chart.seriesMuted(color)
    val maxValue = values.maxOrNull()?.takeIf { it > 0L } ?: 1L
    Canvas(
        modifier
            .fillMaxWidth()
            .height(barAreaHeight + StatisticsMetrics.rhythmLabelArea)
            .semantics { contentDescription = description },
    ) {
        val slot = size.width / values.size
        val barWidth = minOf(slot * BAR_WIDTH_FRACTION, StatisticsMetrics.barMaxWidth.toPx())
        val barBottom = barAreaHeight.toPx()
        val radius = CornerRadius(StatisticsMetrics.rhythmBarCorner.toPx())
        values.forEachIndexed { index, value ->
            val left = slot * index + (slot - barWidth) / 2f
            drawRoundRect(trackColor, Offset(left, 0f), Size(barWidth, barBottom), radius)
            if (value > 0L) {
                val height = (barBottom * value / maxValue).coerceAtLeast(StatisticsMetrics.rhythmBarMinHeight.toPx())
                drawRoundRect(
                    color = if (index == highlightIndex) color else mutedColor,
                    topLeft = Offset(left, barBottom - height),
                    size = Size(barWidth, height),
                    cornerRadius = radius,
                )
            }
        }
        drawSlotLabels(labels, slot, barBottom, textMeasurer, labelStyle, labelColor)
    }
}

/** Draws each label centred under its slot below [top], kept inside the canvas at both ends. */
internal fun DrawScope.drawSlotLabels(
    labels: List<String?>,
    slot: Float,
    top: Float,
    textMeasurer: TextMeasurer,
    style: TextStyle,
    color: Color,
) {
    labels.forEachIndexed { index, label ->
        if (label == null) return@forEachIndexed
        val layout = textMeasurer.measure(label, style)
        val x = (slot * index + slot / 2f - layout.size.width / 2f)
            .coerceIn(0f, (size.width - layout.size.width).coerceAtLeast(0f))
        drawText(layout, color = color, topLeft = Offset(x, top + LABEL_GAP.toPx()))
    }
}

/** Width of a bar in its slot, so neighbouring bars keep a gap. */
internal const val BAR_WIDTH_FRACTION = 0.7f
private val LABEL_GAP = KatariTheme.spacing.xs
