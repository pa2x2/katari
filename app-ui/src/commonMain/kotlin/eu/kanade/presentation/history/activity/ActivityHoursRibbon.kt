package eu.kanade.presentation.history.activity

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import app.katari.presentation.kit.content.StatisticsMetrics
import app.katari.presentation.kit.content.chartBar
import app.katari.presentation.kit.theme.KatariTheme
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.ui.history.activity.HistoryActivitySpan

/**
 * A day's 24 hours across, filled in each type's colour where time was spent, with marks at 6, 12 and 18 o'clock.
 * The day's span is written beside it, so the drawing carries nothing a screen reader would miss.
 */
@Composable
internal fun ActivityHoursRibbon(
    spans: List<HistoryActivitySpan>,
    colors: Map<EntryType, Color>,
    modifier: Modifier = Modifier,
) {
    val track = KatariTheme.chart.track
    val marks = KatariTheme.chart.outline
    val fallback = KatariTheme.chart.series
    val minWidth = KatariTheme.spacing.xxs
    Canvas(modifier.height(StatisticsMetrics.hoursRibbon).chartBar()) {
        drawRect(track)
        MARKED_HOURS.forEach { hour ->
            val x = size.width * hour / HOURS_PER_DAY
            drawLine(marks, Offset(x, 0f), Offset(x, size.height))
        }
        spans.forEach { span ->
            val left = size.width * span.startMinute / MINUTES_PER_DAY
            val width = maxOf(size.width * (span.endMinute - span.startMinute) / MINUTES_PER_DAY, minWidth.toPx())
            drawRect(colors[span.type] ?: fallback, Offset(left, 0f), Size(width, size.height))
        }
    }
}

private val MARKED_HOURS = listOf(6, 12, 18)
private const val HOURS_PER_DAY = 24f
private const val MINUTES_PER_DAY = 24f * 60f
