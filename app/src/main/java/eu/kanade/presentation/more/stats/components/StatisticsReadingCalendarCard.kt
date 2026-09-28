package eu.kanade.presentation.more.stats.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.more.stats.data.StatsReadingCalendar
import eu.kanade.tachiyomi.source.entry.EntryType
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import kotlin.math.ceil

/**
 * Twelve months of daily time ending today, independent of the selected range. [color] is the Overview
 * primary or the selected type's accent; days before [trackingStartDate] are hatched like the chart.
 */
@Composable
internal fun StatisticsReadingCalendarCard(
    calendar: StatsReadingCalendar,
    type: EntryType?,
    color: Color,
    trackingStartDate: LocalDate?,
    formatDuration: (Long) -> String,
    onOpenDay: (LocalDate) -> Unit,
) {
    val locale = LocalConfiguration.current.locales[0]
    val firstDayOfWeek = WeekFields.of(locale).firstDayOfWeek
    val weeks = remember(calendar.startDate, calendar.endDate, firstDayOfWeek) {
        readingCalendarWeeks(calendar.startDate, calendar.endDate, firstDayOfWeek)
    }
    val durations = remember(calendar, type) {
        weeks.flatMap { it.days }.filterNotNull().associateWith { calendar.durationOn(it, type) }
    }
    val maxDuration = durations.values.maxOrNull() ?: 0L
    val activeDays = durations.values.count { it > 0L }
    val dayFormatter = remember(locale) { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale) }
    val monthFormatter = remember(locale) { DateTimeFormatter.ofPattern("LLL", locale) }
    val emptyColor = MaterialTheme.colorScheme.surfaceVariant
    val hatchColor = MaterialTheme.colorScheme.outlineVariant
    val scrollState = rememberScrollState()
    LaunchedEffect(scrollState.maxValue) { scrollState.scrollTo(scrollState.maxValue) }

    StatisticsSectionCard(
        title = stringResource(MR.strings.statistics_reading_calendar),
        trailingText = stringResource(MR.strings.statistics_last_12_months),
        contentSpacing = 12.dp,
    ) {
        Row {
            Column(Modifier.padding(top = MONTH_LABEL_HEIGHT, end = 6.dp)) {
                (0L until 7L).forEach { offset ->
                    Box(Modifier.height(CELL_SIZE + CELL_GAP), contentAlignment = Alignment.CenterStart) {
                        // Every other weekday keeps the column narrow while still anchoring the rows.
                        if (offset % 2L == 0L) {
                            Text(
                                text = firstDayOfWeek.plus(offset).getDisplayName(TextStyle.SHORT, locale),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            Row(
                modifier = Modifier
                    .weight(1f)
                    .nestedScroll(StatisticsTrendChartPagerIsolation)
                    .horizontalScroll(scrollState),
                horizontalArrangement = Arrangement.spacedBy(CELL_GAP),
            ) {
                weeks.forEach { week ->
                    Column(verticalArrangement = Arrangement.spacedBy(CELL_GAP)) {
                        Box(Modifier.width(CELL_SIZE).height(MONTH_LABEL_HEIGHT - CELL_GAP)) {
                            week.startsMonth?.let { month ->
                                Text(
                                    text = month.format(monthFormatter),
                                    modifier = Modifier.wrapContentWidth(Alignment.Start, unbounded = true),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                )
                            }
                        }
                        week.days.forEach { day ->
                            if (day == null) {
                                Spacer(Modifier.size(CELL_SIZE))
                                return@forEach
                            }
                            val duration = durations[day] ?: 0L
                            val isUntracked = trackingStartDate == null || day.isBefore(trackingStartDate)
                            val label = "${day.format(dayFormatter)}: ${formatDuration(duration)}"
                            Box(
                                Modifier
                                    .size(CELL_SIZE)
                                    .clip(CELL_SHAPE)
                                    .background(intensityColor(duration, maxDuration, color, emptyColor))
                                    .then(
                                        if (isUntracked && duration == 0L) {
                                            Modifier.drawBehind {
                                                drawUntrackedHatch(0f, size.width, 0f, size.height, hatchColor)
                                            }
                                        } else {
                                            Modifier
                                        },
                                    )
                                    .then(
                                        if (duration > 0L) {
                                            Modifier.clickable(onClickLabel = label) { onOpenDay(day) }
                                        } else {
                                            Modifier
                                        },
                                    )
                                    .semantics { contentDescription = label },
                            )
                        }
                    }
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = pluralStringResource(MR.plurals.statistics_active_day_count, activeDays, activeDays) +
                    " · " + formatDuration(durations.values.sum()),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            CalendarLegend(color, emptyColor)
        }
    }
}

@Composable
private fun CalendarLegend(color: Color, emptyColor: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
            text = stringResource(MR.strings.statistics_less),
            modifier = Modifier.padding(end = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        (0..INTENSITY_LEVELS).forEach { level ->
            Box(
                Modifier
                    .size(LEGEND_CELL_SIZE)
                    .clip(CELL_SHAPE)
                    .background(levelColor(level, color, emptyColor)),
            )
        }
        Text(
            text = stringResource(MR.strings.statistics_more),
            modifier = Modifier.padding(start = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Levels are relative to the busiest day shown, so a light reader's calendar still shows contrast. */
private fun intensityColor(duration: Long, maxDuration: Long, color: Color, emptyColor: Color): Color {
    if (duration <= 0L || maxDuration <= 0L) return emptyColor
    val level = ceil(duration.toDouble() * INTENSITY_LEVELS / maxDuration).toInt().coerceIn(1, INTENSITY_LEVELS)
    return levelColor(level, color, emptyColor)
}

// Blending from the empty cell keeps the lowest level distinct on both light and dark surfaces.
private fun levelColor(level: Int, color: Color, emptyColor: Color): Color =
    if (level == 0) emptyColor else lerp(emptyColor, color, LEVEL_FRACTIONS[level - 1])

private const val INTENSITY_LEVELS = 4
private val LEVEL_FRACTIONS = floatArrayOf(0.35f, 0.55f, 0.78f, 1f)
private val CELL_SIZE = 14.dp
private val CELL_GAP = 3.dp
private val LEGEND_CELL_SIZE = 10.dp
private val MONTH_LABEL_HEIGHT = 18.dp
private val CELL_SHAPE = RoundedCornerShape(3.dp)
