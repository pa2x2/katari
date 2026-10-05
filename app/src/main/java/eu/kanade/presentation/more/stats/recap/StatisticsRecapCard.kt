package eu.kanade.presentation.more.stats.recap

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.entry.components.EntryCover
import eu.kanade.presentation.more.stats.components.StatisticsRhythmBars
import eu.kanade.presentation.more.stats.components.rememberStatisticsDurationFormatter
import eu.kanade.tachiyomi.ui.stats.recap.StatisticsRecap
import eu.kanade.tachiyomi.ui.stats.recap.consumedCountsText
import tachiyomi.domain.statistics.model.StatisticsTopEntry
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource
import java.text.NumberFormat
import java.time.format.TextStyle
import java.time.temporal.WeekFields

/**
 * The image a recap is shared as; drawn at a fixed width so it looks the same on every screen, and in the portrait
 * shape of a story so it fills one.
 */
@Composable
internal fun StatisticsRecapCard(
    recap: StatisticsRecap,
    periodLabel: String,
    typeLabel: String?,
    modifier: Modifier = Modifier,
) {
    val formatDuration = rememberStatisticsDurationFormatter()
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .width(CARD_WIDTH)
            .heightIn(min = CARD_WIDTH * 16 / 9)
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.verticalGradient(listOf(colors.primaryContainer, colors.surfaceContainerHigh)))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
    ) {
        Column {
            Text(
                text = listOfNotNull(stringResource(MR.strings.app_name), periodLabel, typeLabel).joinToString(" · "),
                style = MaterialTheme.typography.labelLarge,
                color = colors.onPrimaryContainer,
            )
            Text(
                text = formatDuration(recap.totalDurationMillis),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp),
            )
            recap.consumedCountsText(LocalContext.current)?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                )
            }
        }
        if (recap.topTitles.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                recap.topTitles.forEachIndexed { index, title ->
                    TopTitleRow(rank = index + 1, title = title, duration = formatDuration(title.durationMillis))
                }
            }
        }
        WeekdayRhythm(recap)
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            RecapFigure(
                value = NumberFormat.getIntegerInstance(LocalConfiguration.current.locales[0])
                    .format(recap.activeDays),
                label = stringResource(MR.strings.statistics_active_days),
            )
            RecapFigure(
                value = pluralStringResource(MR.plurals.day, recap.longestStreakDays, recap.longestStreakDays),
                label = stringResource(MR.strings.statistics_recap_longest_streak),
            )
        }
    }
}

@Composable
private fun TopTitleRow(rank: Int, title: StatisticsTopEntry, duration: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = rank.toString(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.width(20.dp),
        )
        EntryCover.Book(data = title.cover, modifier = Modifier.width(44.dp))
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(
                text = title.title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = duration,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun WeekdayRhythm(recap: StatisticsRecap) {
    val busiest = recap.busiestWeekday ?: return
    val locale = LocalConfiguration.current.locales[0]
    val days = remember(locale) {
        val first = WeekFields.of(locale).firstDayOfWeek
        (0L until 7L).map { first.plus(it) }
    }
    val heading = stringResource(MR.strings.statistics_busiest_weekday, busiest.getDisplayName(TextStyle.FULL, locale))
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = heading, style = MaterialTheme.typography.titleSmall)
        StatisticsRhythmBars(
            values = days.map { recap.weekdayDurationMillis[it] ?: 0L },
            labels = days.map { it.getDisplayName(TextStyle.SHORT, locale) },
            highlightIndex = days.indexOf(busiest),
            color = MaterialTheme.colorScheme.primary,
            description = heading,
            barAreaHeight = 48.dp,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun RecapFigure(value: String, label: String) {
    Column {
        Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private val CARD_WIDTH = 320.dp
