package eu.kanade.presentation.more.stats.components

import android.text.format.DateFormat
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import eu.kanade.presentation.more.stats.data.StatsActivity
import mihon.presentation.kit.content.Figure
import mihon.presentation.kit.content.FigureGrid
import mihon.presentation.kit.content.StatisticsMetrics
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.surface.DashboardCard
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
import java.text.NumberFormat
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.WeekFields

/** Session figures plus when in the day and week the time was spent. [color] tints the bars. */
@Composable
internal fun StatisticsActivityPatternsCard(
    activity: StatsActivity?,
    color: Color,
    formatDuration: (Long) -> String,
) {
    val hasSessions = activity != null && activity.sessionCount > 0L
    val dash = "—"
    DashboardCard(title = stringResource(MR.strings.statistics_activity_patterns)) {
        FigureGrid(
            listOf(
                Figure(
                    value = activity?.sessionCount?.let(NumberFormat.getIntegerInstance()::format) ?: dash,
                    label = stringResource(MR.strings.statistics_sessions),
                ),
                Figure(
                    value = activity?.activeDays?.toString() ?: dash,
                    label = stringResource(MR.strings.statistics_active_days),
                ),
                Figure(
                    value = activity?.averageSessionDurationMillis?.takeIf { hasSessions }?.let(formatDuration) ?: dash,
                    label = stringResource(MR.strings.statistics_average_session),
                ),
                Figure(
                    value = activity?.longestSessionDurationMillis?.takeIf { hasSessions }?.let(formatDuration) ?: dash,
                    label = stringResource(MR.strings.statistics_longest_session),
                ),
            ),
        )
        activity?.rhythm?.takeIf { it.hasActivity }?.let { rhythm ->
            ActivityRhythm(rhythm.hourlyDurationMillis, rhythm.weekdayDurationMillis, color)
        }
    }
}

@Composable
private fun ActivityRhythm(
    hourly: List<Long>,
    weekdays: Map<DayOfWeek, Long>,
    color: Color,
) {
    val locale = LocalConfiguration.current.locales[0]
    // "j" resolves to the locale's preferred hour cycle, e.g. "9 PM" or "21".
    val hourFormatter = remember(locale) {
        DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, "j"), locale)
    }
    val formatHour = { hour: Int -> LocalTime.of(hour, 0).format(hourFormatter) }
    val peak = peakHour(hourly)
    val headline = listOfNotNull(
        dominantPartOfDay(hourly)?.let { stringResource(it.mostlyLabel) },
        peak?.let { stringResource(MR.strings.statistics_peak_hour, formatHour(it)) },
    ).joinToString(" · ")
    val orderedDays = remember(locale) {
        val first = WeekFields.of(locale).firstDayOfWeek
        (0L until 7L).map { first.plus(it) }
    }
    val dayValues = orderedDays.map { weekdays[it] ?: 0L }
    val busiestIndex = dayValues.indices.filter { dayValues[it] > 0L }.maxByOrNull { dayValues[it] }
    val busiestDay = busiestIndex?.let {
        stringResource(MR.strings.statistics_busiest_weekday, orderedDays[it].getDisplayName(TextStyle.FULL, locale))
    }

    Text(
        text = headline,
        modifier = Modifier.padding(top = KatariTheme.spacing.xl, bottom = KatariTheme.spacing.m),
        role = TextRole.Label,
    )
    StatisticsRhythmBars(
        values = hourly,
        labels = hourly.indices.map { hour -> formatHour(hour).takeIf { hour % HOUR_LABEL_STEP == 0 } },
        highlightIndex = peak,
        color = color,
        description = headline,
        barAreaHeight = StatisticsMetrics.rhythmHeight,
    )
    busiestDay?.let {
        Text(
            text = it,
            modifier = Modifier.padding(top = KatariTheme.spacing.l, bottom = KatariTheme.spacing.m),
            role = TextRole.Label,
        )
        StatisticsRhythmBars(
            values = dayValues,
            labels = orderedDays.map { day -> day.getDisplayName(TextStyle.SHORT, locale) },
            highlightIndex = busiestIndex,
            color = color,
            description = it,
            barAreaHeight = StatisticsMetrics.rhythmCompactHeight,
        )
    }
}

private const val HOUR_LABEL_STEP = 6
