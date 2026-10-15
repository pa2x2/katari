package eu.kanade.presentation.more.stats.recap.components.calendar

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
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
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import eu.kanade.presentation.more.stats.recap.components.LocalRecapPalette
import eu.kanade.presentation.more.stats.recap.components.rememberRecapFormats
import eu.kanade.presentation.more.stats.recap.motion.LocalRecapClock
import eu.kanade.presentation.more.stats.recap.motion.RecapClock
import eu.kanade.presentation.more.stats.recap.motion.rememberRecapEntrance
import mihon.presentation.kit.content.StoryMetrics
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.content.drawingStyle
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.StoryPalette
import mihon.presentation.kit.theme.TextRole
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
        measurer = rememberTextMeasurer(),
        dayStyle = TextRole.ItemTitle.drawingStyle,
        runDayStyle = TextRole.GroupLabel.drawingStyle,
    )
    val weekdays = remember(firstDayOfWeek, formats) {
        List(DAYS_IN_WEEK) { firstDayOfWeek.plus(it.toLong()) }
            .map { it.getDisplayName(DateTextStyle.NARROW_STANDALONE, formats.locale) }
    }
    ShrinkToFit(modifier) {
        when (layout) {
            is RecapRunCalendarLayout.Weeks -> Column(
                verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s),
            ) {
                layout.weeks.forEachIndexed { index, days ->
                    layout.labels[index]?.let { months -> MonthLabel(months.joinToString(" – ") { formats.month(it) }) }
                    if (index == 0) WeekdayHeader(weekdays)
                    WeekRow(days, marks, height = StoryMetrics.calendarWeekHeight, numberScale = 1f)
                }
            }
            is RecapRunCalendarLayout.Months -> Column(
                verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.l),
            ) {
                layout.months.chunked(layout.columns).forEach { row ->
                    val gap = if (layout.withNumbers) KatariTheme.spacing.l else KatariTheme.spacing.m
                    Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
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
    Column(verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.xxs), modifier = modifier) {
        MonthLabel(label)
        weekdays?.let { WeekdayHeader(it) }
        month.weeks.forEach { days ->
            if (weekdays != null) {
                WeekRow(days, marks, height = StoryMetrics.calendarMonthWeekHeight, numberScale = MONTH_NUMBER_SCALE)
            } else {
                WeekRow(days, marks, height = StoryMetrics.calendarDotWeekHeight, numberScale = null)
            }
        }
    }
}

@Composable
private fun MonthLabel(text: String) {
    Text(text = text.uppercase(), role = TextRole.Badge, tone = ContentTone.Primary)
}

@Composable
private fun WeekdayHeader(weekdays: List<String>) {
    Row(Modifier.fillMaxWidth()) {
        weekdays.forEach { day ->
            Text(
                text = day,
                modifier = Modifier.weight(1f),
                role = TextRole.Badge,
                tone = ContentTone.Secondary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * One week: the run's pill and the dots drawn under the days, and the day numbers on top. Everything is drawn rather
 * than composed, so the numbers can change colour as the pill fills without recomposing.
 *
 * @param days null for days the row leaves blank.
 * @param numberScale the day numbers' size against a week calendar's; null draws every day as a dot instead.
 */
@Composable
private fun WeekRow(days: List<LocalDate?>, marks: RunMarks, height: Dp, numberScale: Float?) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(height)
            .drawBehind {
                with(marks) {
                    drawWeek(days, numbered = numberScale != null)
                    if (numberScale != null) drawNumbers(days, numberScale)
                }
            },
    )
}

/** What the calendar marks and how far the run has filled in, read while drawing. */
private class RunMarks(
    val run: ClosedRange<LocalDate>,
    val activeDates: Set<LocalDate>,
    val period: ClosedRange<LocalDate>,
    val fill: State<Float>,
    val clock: RecapClock,
    val palette: StoryPalette,
    private val measurer: TextMeasurer,
    private val dayStyle: TextStyle,
    private val runDayStyle: TextStyle,
) {
    private val runDays = ChronoUnit.DAYS.between(run.start, run.endInclusive) + 1f

    /** How many of the run's days, from its first, the pill covers so far. */
    private val filledDays get() = fill.value * runDays

    private fun dayIndex(day: LocalDate) = ChronoUnit.DAYS.between(run.start, day)

    private fun numberColor(day: LocalDate): Color = when {
        day in run && dayIndex(day) + 0.5f < filledDays -> palette.background
        day !in period -> palette.mutedFaint
        else -> palette.muted
    }

    /** The day numbers, centred in their columns; days of the run are set heavier. */
    fun DrawScope.drawNumbers(days: List<LocalDate?>, scale: Float) {
        val cell = size.width / days.size
        days.forEachIndexed { column, day ->
            if (day == null) return@forEachIndexed
            val style = if (day in run) runDayStyle else dayStyle
            val layout = measurer.measure(day.dayOfMonth.toString(), style.copy(fontSize = style.fontSize * scale))
            drawText(
                layout,
                color = numberColor(day),
                topLeft = Offset(
                    (column + 0.5f) * cell - layout.size.width / 2f,
                    (size.height - layout.size.height) / 2f,
                ),
            )
        }
    }

    fun DrawScope.drawWeek(days: List<LocalDate?>, numbered: Boolean) {
        val cell = size.width / days.size
        val runColumns = days.indices.filter { days[it]?.let { day -> day in run } == true }
        runColumns.firstOrNull()?.let { column -> drawPill(column, runColumns.size, dayIndex(days[column]!!), cell) }
        val dotY = if (numbered) size.height - StoryMetrics.calendarDotInset.toPx() else size.height / 2f
        days.forEachIndexed { column, day ->
            if (day == null || day in run) return@forEachIndexed
            val active = day in activeDates
            val center = Offset((column + 0.5f) * cell, dotY)
            when {
                numbered && active -> drawCircle(palette.accent, StoryMetrics.calendarDotRadius.toPx(), center)
                numbered -> Unit
                active -> drawCircle(palette.accent, StoryMetrics.calendarActiveDotRadius.toPx(), center)
                day in period -> drawCircle(palette.inkFaint, StoryMetrics.calendarDayDotRadius.toPx(), center)
            }
        }
    }

    /** The run's part of a week: [count] days from [column], the first of them the run's day [firstDay]. */
    private fun DrawScope.drawPill(column: Int, count: Int, firstDay: Long, cell: Float) {
        val shown = (filledDays - firstDay).coerceIn(0f, count.toFloat())
        if (shown <= 0f) return
        val inset = StoryMetrics.calendarPillInset.toPx()
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
                    listOf(Color.Transparent, palette.glint, Color.Transparent),
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

/** Month calendars set their day numbers smaller than a calendar of weeks, so two months fit side by side. */
private const val MONTH_NUMBER_SCALE = 10f / 14f
