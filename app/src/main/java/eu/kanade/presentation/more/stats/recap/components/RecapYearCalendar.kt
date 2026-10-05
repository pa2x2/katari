package eu.kanade.presentation.more.stats.recap.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.YearMonth

/**
 * The period as one row of days per month, so a run shows up as a solid streak among the gaps. Days of the run are
 * drawn in ink, other active days in accent, the rest faintly; days the period doesn't cover are left out.
 */
@Composable
internal fun RecapYearCalendar(
    periodStart: LocalDate,
    periodEnd: LocalDate,
    activeDates: Set<LocalDate>,
    run: ClosedRange<LocalDate>,
    monthLabel: (YearMonth) -> String,
    modifier: Modifier = Modifier,
) {
    val palette = LocalRecapPalette.current
    val reveal = LocalRecapReveal.current
    val measurer = rememberTextMeasurer()
    val months = generateSequence(YearMonth.from(periodStart)) { it.plusMonths(1L) }
        .takeWhile { !it.isAfter(YearMonth.from(periodEnd)) }
        .toList()
    Canvas(modifier.fillMaxWidth().height(ROW_HEIGHT * months.size)) {
        val labelWidth = LABEL_WIDTH.toPx()
        val cell = (size.width - labelWidth) / DAYS_IN_ROW
        val rowHeight = ROW_HEIGHT.toPx()
        val dot = Size(cell * 0.78f, rowHeight * 0.62f)
        val radius = CornerRadius(dot.width * 0.25f)
        months.forEachIndexed { row, month ->
            val y = row * rowHeight
            val label = measurer.measure(monthLabel(month), RecapTypography.Tiny.copy(color = palette.muted))
            drawText(label, topLeft = Offset(0f, y + (rowHeight - label.size.height) / 2f))
            val shownDays = (month.lengthOfMonth() * reveal.value.coerceIn(0f, 1f)).toInt()
            (1..month.lengthOfMonth()).forEach { day ->
                val date = month.atDay(day)
                if (date.isBefore(periodStart) || date.isAfter(periodEnd)) return@forEach
                val color = when {
                    day > shownDays -> palette.faint
                    date in run -> palette.ink
                    date in activeDates -> palette.accent.copy(alpha = 0.7f)
                    else -> palette.faint
                }
                drawRoundRect(
                    color = color,
                    topLeft = Offset(labelWidth + (day - 1) * cell, y + (rowHeight - dot.height) / 2f),
                    size = dot,
                    cornerRadius = radius,
                )
            }
        }
    }
}

private val ROW_HEIGHT = 13.dp
private val LABEL_WIDTH = 16.dp
private const val DAYS_IN_ROW = 31
