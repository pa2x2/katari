package eu.kanade.presentation.more.stats.components

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.more.stats.data.StatsActivity
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
    StatisticsSectionCard(title = stringResource(MR.strings.statistics_activity_patterns)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            PatternTileRow(
                first = activity?.sessionCount?.let(NumberFormat.getIntegerInstance()::format) ?: dash,
                firstLabel = stringResource(MR.strings.statistics_sessions),
                second = activity?.activeDays?.toString() ?: dash,
                secondLabel = stringResource(MR.strings.statistics_active_days),
            )
            PatternTileRow(
                first = activity?.averageSessionDurationMillis?.takeIf { hasSessions }?.let(formatDuration) ?: dash,
                firstLabel = stringResource(MR.strings.statistics_average_session),
                second = activity?.longestSessionDurationMillis?.takeIf { hasSessions }?.let(formatDuration) ?: dash,
                secondLabel = stringResource(MR.strings.statistics_longest_session),
            )
        }
        activity?.rhythm?.takeIf { it.hasActivity }?.let { rhythm ->
            ActivityRhythm(rhythm.hourlyDurationMillis, rhythm.weekdayDurationMillis, color)
        }
    }
}

@Composable
private fun PatternTileRow(first: String, firstLabel: String, second: String, secondLabel: String) {
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatisticsInsightTile(first, firstLabel, Modifier.weight(1f).fillMaxHeight())
        StatisticsInsightTile(second, secondLabel, Modifier.weight(1f).fillMaxHeight())
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
        modifier = Modifier.padding(top = 20.dp, bottom = 10.dp),
        style = MaterialTheme.typography.titleSmall,
    )
    StatisticsRhythmBars(
        values = hourly,
        labels = hourly.indices.map { hour -> formatHour(hour).takeIf { hour % HOUR_LABEL_STEP == 0 } },
        highlightIndex = peak,
        color = color,
        description = headline,
        barAreaHeight = 64.dp,
    )
    busiestDay?.let {
        Text(
            text = it,
            modifier = Modifier.padding(top = 16.dp, bottom = 10.dp),
            style = MaterialTheme.typography.titleSmall,
        )
        StatisticsRhythmBars(
            values = dayValues,
            labels = orderedDays.map { day -> day.getDisplayName(TextStyle.SHORT, locale) },
            highlightIndex = busiestIndex,
            color = color,
            description = it,
            barAreaHeight = 48.dp,
        )
    }
}

private const val HOUR_LABEL_STEP = 6
