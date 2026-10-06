package eu.kanade.presentation.more.stats.recap.components.calendar

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import eu.kanade.presentation.more.stats.recap.components.LocalRecapPalette
import eu.kanade.presentation.more.stats.recap.components.RecapTypography
import eu.kanade.presentation.more.stats.recap.components.rememberRecapFormats
import eu.kanade.presentation.more.stats.recap.motion.LocalRecapClock
import eu.kanade.presentation.more.stats.recap.motion.RecapClock
import eu.kanade.presentation.more.stats.recap.motion.rememberRecapEntrance
import eu.kanade.presentation.more.stats.recap.palette.RecapPalette
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.time.temporal.WeekFields
import kotlin.math.min
import java.time.format.TextStyle as DateTextStyle

/**
 * The longest run on a calendar laid out by [recapRunCalendarLayout]. The run is a pill along each of its weeks that
 * fills day by day at step [order] of the page's entrance; once full, a light runs along it every few seconds. Other
 * active days are dots, and days outside the period are faint. It shrinks to fit the height it's given.
 */
@Composable
internal fun RecapRunCalendar(
    run: ClosedRange<LocalDate>,
    activeDates: Set<LocalDate>,
    periodStart: LocalDate,
    periodEnd: LocalDate,
    order: Int,
    modifier: Modifier = Modifier,
) {
    val formats = rememberRecapFormats()
    val firstDayOfWeek = WeekFields.of(formats.locale).firstDayOfWeek
    val layout = remember(run, periodStart, periodEnd, firstDayOfWeek) {
        recapRunCalendarLayout(run, periodStart, periodEnd, firstDayOfWeek)
    }
    val marks = RunMarks(
        run = run,
        activeDates = activeDates,
        period = periodStart..periodEnd,
        fill = rememberRecapEntrance(order, FILL_MILLIS, FastOutSlowInEasing),
        clock = LocalRecapClock.current,
        palette = LocalRecapPalette.current,
    )
    val weekdays = remember(firstDayOfWeek, formats) {
        List(DAYS_IN_WEEK) { firstDayOfWeek.plus(it.toLong()) }
            .map { it.getDisplayName(DateTextStyle.NARROW_STANDALONE, formats.locale) }
    }
    ShrinkToFit(modifier) {
        when (layout) {
            is RecapRunCalendarLayout.Weeks -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                layout.weeks.forEachIndexed { index, days ->
                    layout.labels[index]?.let { months -> MonthLabel(months.joinToString(" – ") { formats.month(it) }) }
                    if (index == 0) WeekdayHeader(weekdays, fontSize = 11)
                    WeekRow(days, marks, height = 34.dp, fontSize = 14)
                }
            }
            is RecapRunCalendarLayout.Months -> Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                layout.months.chunked(layout.columns).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(if (layout.withNumbers) 16.dp else 12.dp)) {
                        row.forEach { month ->
                            MonthCalendar(
                                month = month,
                                label = if (layout.withNumbers) {
                                    formats.month(month.month)
                                } else {
                                    formats.shortMonth(month.month)
                                },
                                weekdays = weekdays.takeIf { layout.withNumbers },
                                marks = marks,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        repeat(layout.columns - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}

/** @param weekdays the header's letters; without them the days are dots rather than numbers. */
@Composable
private fun MonthCalendar(
    month: RecapRunCalendarLayout.Month,
    label: String,
    weekdays: List<String>?,
    marks: RunMarks,
    modifier: Modifier = Modifier,
) {
    Column(verticalArrangement = Arrangement.spacedBy(if (weekdays != null) 3.dp else 2.dp), modifier = modifier) {
        MonthLabel(label)
        weekdays?.let { WeekdayHeader(it, fontSize = 9) }
        month.weeks.forEach { days ->
            if (weekdays != null) {
                WeekRow(days, marks, height = 20.dp, fontSize = 10)
            } else {
                WeekRow(days, marks, height = 12.dp, fontSize = null)
            }
        }
    }
}

@Composable
private fun MonthLabel(text: String) {
    BasicText(
        text = text.uppercase(),
        style = RecapTypography.Tiny.copy(color = LocalRecapPalette.current.accent, letterSpacing = 0.1.em),
    )
}

@Composable
private fun WeekdayHeader(weekdays: List<String>, fontSize: Int) {
    val palette = LocalRecapPalette.current
    Row(Modifier.fillMaxWidth()) {
        weekdays.forEach { day ->
            BasicText(
                text = day,
                style = TextStyle(fontSize = fontSize.sp, textAlign = TextAlign.Center, color = palette.muted),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/**
 * One week: the run's pill and the dots drawn under the days, and the day numbers on top.
 *
 * @param days null for days the row leaves blank.
 * @param fontSize the day numbers' size; null draws every day as a dot instead.
 */
@Composable
private fun WeekRow(days: List<LocalDate?>, marks: RunMarks, height: Dp, fontSize: Int?) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(height)
            .drawBehind { with(marks) { drawWeek(days, numbered = fontSize != null) } },
    ) {
        if (fontSize == null) return@Box
        Row(Modifier.fillMaxWidth().align(Alignment.Center)) {
            days.forEach { day ->
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    if (day == null) return@Box
                    val inRun = day in marks.run
                    BasicText(
                        text = day.dayOfMonth.toString(),
                        style = TextStyle(
                            fontSize = fontSize.sp,
                            fontWeight = if (inRun) FontWeight.ExtraBold else FontWeight.Medium,
                        ),
                        color = { marks.numberColor(day) },
                    )
                }
            }
        }
    }
}

/** What the calendar marks and how far the run has filled in, read while drawing. */
private class RunMarks(
    val run: ClosedRange<LocalDate>,
    val activeDates: Set<LocalDate>,
    val period: ClosedRange<LocalDate>,
    val fill: State<Float>,
    val clock: RecapClock,
    val palette: RecapPalette,
) {
    private val runDays = ChronoUnit.DAYS.between(run.start, run.endInclusive) + 1f

    /** How many of the run's days, from its first, the pill covers so far. */
    private val filledDays get() = fill.value * runDays

    private fun dayIndex(day: LocalDate) = ChronoUnit.DAYS.between(run.start, day)

    fun numberColor(day: LocalDate): Color = when {
        day in run && dayIndex(day) + 0.5f < filledDays -> palette.background
        day !in period -> palette.muted.copy(alpha = 0.35f)
        else -> palette.muted
    }

    fun DrawScope.drawWeek(days: List<LocalDate?>, numbered: Boolean) {
        val cell = size.width / days.size
        val runColumns = days.indices.filter { days[it]?.let { day -> day in run } == true }
        runColumns.firstOrNull()?.let { column -> drawPill(column, runColumns.size, dayIndex(days[column]!!), cell) }
        val dotY = if (numbered) size.height - DOT_INSET.toPx() else size.height / 2f
        days.forEachIndexed { column, day ->
            if (day == null || day in run) return@forEachIndexed
            val active = day in activeDates
            val center = Offset((column + 0.5f) * cell, dotY)
            when {
                numbered && active -> drawCircle(palette.accent, radius = 2.dp.toPx(), center = center)
                numbered -> Unit
                active -> drawCircle(palette.accent, radius = 2.5.dp.toPx(), center = center)
                day in period -> drawCircle(palette.ink.copy(alpha = 0.22f), radius = 1.5.dp.toPx(), center = center)
            }
        }
    }

    /** The run's part of a week: [count] days from [column], the first of them the run's day [firstDay]. */
    private fun DrawScope.drawPill(column: Int, count: Int, firstDay: Long, cell: Float) {
        val shown = (filledDays - firstDay).coerceIn(0f, count.toFloat())
        if (shown <= 0f) return
        val inset = PILL_INSET.toPx()
        val pill = RoundRect(
            left = column * cell + inset,
            top = 0f,
            right = (column + shown) * cell - inset,
            bottom = size.height,
            cornerRadius = CornerRadius(size.height / 2f),
        )
        val path = Path().apply { addRoundRect(pill) }
        drawPath(path, palette.ink)
        if (fill.value < 1f) return
        val sweep = (clock.storyMillis % GLINT_PERIOD_MILLIS) / GLINT_SWEEP_MILLIS.toFloat()
        if (sweep >= 1f) return
        // The light travels the whole run, so it crosses one week after another.
        val glintDay = -GLINT_DAYS + (runDays + 2 * GLINT_DAYS) * sweep
        val x = (column + glintDay - firstDay) * cell
        val width = GLINT_DAYS * cell
        clipPath(path) {
            drawRect(
                brush = Brush.horizontalGradient(
                    listOf(Color.Transparent, Color.White.copy(alpha = 0.7f), Color.Transparent),
                    startX = x - width,
                    endX = x + width,
                ),
                topLeft = Offset(x - width, 0f),
                size = Size(width * 2, size.height),
            )
        }
    }
}

/** Measures [content] at its natural height and scales it down, never up, to fit the height given. */
@Composable
private fun ShrinkToFit(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Layout(content = content, modifier = modifier) { measurables, constraints ->
        val placeable = measurables.single()
            .measure(constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity))
        val height = if (constraints.hasBoundedHeight) constraints.maxHeight else placeable.height
        val scale = min(1f, height / placeable.height.toFloat())
        layout(constraints.maxWidth, height) {
            // Scaling happens around the content's center, so centering its unscaled box centers the scaled one.
            placeable.placeWithLayer(
                x = (constraints.maxWidth - placeable.width) / 2,
                y = (height - placeable.height) / 2,
            ) {
                scaleX = scale
                scaleY = scale
            }
        }
    }
}

private const val DAYS_IN_WEEK = 7
private const val FILL_MILLIS = 1_500
private const val GLINT_PERIOD_MILLIS = 3_200L
private const val GLINT_SWEEP_MILLIS = 1_800L
private const val GLINT_DAYS = 1.4f
private val PILL_INSET = 1.5.dp
private val DOT_INSET = 3.dp
