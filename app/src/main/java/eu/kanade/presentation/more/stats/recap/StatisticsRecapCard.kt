package eu.kanade.presentation.more.stats.recap

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.entry.components.EntryCover
import eu.kanade.presentation.more.stats.components.rememberStatisticsDurationFormatter
import eu.kanade.tachiyomi.ui.stats.recap.StatisticsRecap
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource
import java.time.format.TextStyle

/** The image a recap is shared as; drawn at a fixed width so it looks the same on every screen. */
@Composable
internal fun StatisticsRecapCard(
    recap: StatisticsRecap,
    periodLabel: String,
    typeLabel: String?,
    modifier: Modifier = Modifier,
) {
    val formatDuration = rememberStatisticsDurationFormatter()
    val locale = LocalConfiguration.current.locales[0]
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .width(320.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.verticalGradient(listOf(colors.primaryContainer, colors.surface)))
            .padding(20.dp),
    ) {
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
        Text(
            text = listOf(
                pluralStringResource(
                    MR.plurals.statistics_completion_count,
                    recap.completionCount.toInt(),
                    recap.completionCount.toInt(),
                ),
                pluralStringResource(MR.plurals.statistics_active_day_count, recap.activeDays, recap.activeDays),
            ).joinToString(" · "),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant,
        )
        if (recap.topTitles.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                recap.topTitles.forEachIndexed { index, title ->
                    Column(Modifier.weight(1f)) {
                        Box {
                            EntryCover.Book(data = title.cover, modifier = Modifier.fillMaxWidth())
                            Surface(
                                color = colors.scrim.copy(alpha = 0.6f),
                                contentColor = colors.inverseOnSurface,
                                shape = MaterialTheme.shapes.extraSmall,
                                modifier = Modifier.align(Alignment.TopStart).padding(4.dp),
                            ) {
                                Text(
                                    text = "${index + 1}",
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.padding(horizontal = 6.dp),
                                )
                            }
                        }
                        Text(
                            text = title.title,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            RecapFigure(
                value = pluralStringResource(MR.plurals.day, recap.longestStreakDays, recap.longestStreakDays),
                label = stringResource(MR.strings.statistics_recap_longest_streak),
            )
            recap.busiestWeekday?.let { weekday ->
                RecapFigure(
                    value = weekday.getDisplayName(TextStyle.FULL_STANDALONE, locale),
                    label = stringResource(MR.strings.statistics_recap_busiest_day),
                )
            }
        }
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
