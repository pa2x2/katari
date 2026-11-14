package eu.kanade.presentation.more.stats.components

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLocale
import app.katari.presentation.kit.content.StatisticsMetrics
import app.katari.presentation.kit.content.Text
import app.katari.presentation.kit.surface.DashboardCard
import app.katari.presentation.kit.theme.KatariTheme
import app.katari.presentation.kit.theme.TextRole
import eu.kanade.domain.ui.bestDateTimePattern
import eu.kanade.presentation.more.stats.data.StatsActivityRhythm
import eu.kanade.presentation.util.rememberHourSkeleton
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.WeekFields

/**
 * When in the day and the week the period's time was spent: each chart under the plain sentence it shows, such as
 * "Mostly evenings, around 8 PM", which also names the card. [color] tints the bars.
 */
@Composable
internal fun StatisticsWhenYouReadCard(rhythm: StatsActivityRhythm?, color: Color) {
    if (rhythm == null || !rhythm.hasActivity) {
        DashboardCard(title = stringResource(MR.strings.statistics_when_you_read)) {
            Text(stringResource(MR.strings.statistics_empty_period))
        }
        return
    }
    val locale = LocalLocale.current.platformLocale
    // A bare hour, e.g. "9 PM" or "21"
    val hourSkeleton = rememberHourSkeleton()
    val hourFormatter = remember(locale, hourSkeleton) {
        DateTimeFormatter.ofPattern(bestDateTimePattern(locale, hourSkeleton), locale)
    }
    val formatHour = { hour: Int -> LocalTime.of(hour, 0).format(hourFormatter) }
    val formatDuration = rememberStatisticsDurationFormatter()
    val hourly = rhythm.hourlyDurationMillis
    val partOfDay = dominantPartOfDay(hourly)
    // The busiest hour of the part of day the sentence names, so the highlighted bar is the one it speaks of.
    val peak = hourly.indices
        .filter { hourly[it] > 0L && (partOfDay == null || StatisticsPartOfDay.of(it) == partOfDay) }
        .maxByOrNull { hourly[it] }
    val hoursSentence = when {
        partOfDay != null && peak != null -> stringResource(partOfDay.mostlyLabel, formatHour(peak))
        else -> stringResource(MR.strings.statistics_when_you_read)
    }
    val orderedDays = remember(locale) {
        val first = WeekFields.of(locale).firstDayOfWeek
        (0L until 7L).map { first.plus(it) }
    }
    val dayValues = orderedDays.map { rhythm.weekdayDurationMillis[it] ?: 0L }
    val busiestIndex = dayValues.indices.filter { dayValues[it] > 0L }.maxByOrNull { dayValues[it] }

    DashboardCard(title = hoursSentence) {
        StatisticsRhythmBars(
            values = hourly,
            labels = hourly.indices.map { hour -> formatHour(hour).takeIf { hour % HOUR_LABEL_STEP == 0 } },
            highlightIndex = peak,
            color = color,
            description = hourly.indices.filter { hourly[it] > 0L }
                .joinToString { hour -> "${formatHour(hour)}: ${formatDuration(hourly[hour])}" },
            barAreaHeight = StatisticsMetrics.rhythmHeight,
        )
        busiestIndex?.let { index ->
            val daysSentence = stringResource(
                MR.strings.statistics_busiest_weekday,
                orderedDays[index].getDisplayName(TextStyle.FULL_STANDALONE, locale),
            )
            Text(
                text = daysSentence,
                modifier = Modifier.padding(top = KatariTheme.spacing.l, bottom = KatariTheme.spacing.m),
                role = TextRole.SectionTitle,
            )
            StatisticsRhythmBars(
                values = dayValues,
                labels = orderedDays.map { day -> day.getDisplayName(TextStyle.SHORT, locale) },
                highlightIndex = index,
                color = color,
                description = orderedDays.indices.filter { dayValues[it] > 0L }.joinToString { day ->
                    "${orderedDays[day].getDisplayName(
                        TextStyle.FULL_STANDALONE,
                        locale,
                    )}: ${formatDuration(dayValues[day])}"
                },
                barAreaHeight = StatisticsMetrics.rhythmCompactHeight,
            )
        }
    }
}

private const val HOUR_LABEL_STEP = 6
