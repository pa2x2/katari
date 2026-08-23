package eu.kanade.presentation.more.stats.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.more.stats.data.StatsProgress
import eu.kanade.presentation.more.stats.data.StatsTopTitle
import eu.kanade.presentation.more.stats.data.StatsType
import eu.kanade.tachiyomi.source.entry.EntryType
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun StatisticsHeadlineCards(
    time: String,
    titles: Int,
    thirdValue: String,
    thirdLabel: String,
    timeIcon: ImageVector,
    titlesIcon: ImageVector,
    thirdIcon: ImageVector,
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        maxItemsInEachRow = 3,
    ) {
        StatisticsHeadlineCard(
            value = time,
            label = stringResource(MR.strings.statistics_time_spent),
            icon = timeIcon,
            modifier = Modifier.weight(1f).widthIn(min = 140.dp),
        )
        StatisticsHeadlineCard(
            value = titles.toString(),
            label = stringResource(MR.strings.in_library),
            icon = titlesIcon,
            modifier = Modifier.weight(1f).widthIn(min = 140.dp),
        )
        StatisticsHeadlineCard(
            value = thirdValue,
            label = thirdLabel,
            icon = thirdIcon,
            modifier = Modifier.weight(1f).widthIn(min = 140.dp),
        )
    }
}

@Composable
private fun StatisticsHeadlineCard(
    value: String,
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
) {
    OutlinedCard(modifier = modifier) {
        Column(Modifier.padding(18.dp)) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(20.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
internal fun StatisticsLibraryCard(
    titleCounts: Map<EntryType, Int>,
    types: List<StatsType>,
) {
    StatisticsSectionCard(title = stringResource(MR.strings.statistics_library)) {
        val maximum = titleCounts.values.maxOrNull()?.coerceAtLeast(1) ?: 1
        types.forEach { type ->
            val count = titleCounts[type.type] ?: 0
            val label = stringResource(type.displayName)
            val color = type.accent.color()
            Column(Modifier.semantics { contentDescription = "$label, $count" }) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(label, style = MaterialTheme.typography.bodyMedium)
                    Text(count.toString(), style = MaterialTheme.typography.labelLarge)
                }
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { count.toFloat() / maximum },
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                    color = color,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
internal fun StatisticsProgressCard(progress: StatsProgress?) {
    StatisticsSectionCard(title = stringResource(MR.strings.statistics_progress)) {
        if (progress == null) {
            Text(
                text = stringResource(MR.strings.not_applicable),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@StatisticsSectionCard
        }
        val values = listOf(
            ProgressSlice(
                stringResource(MR.strings.statistics_not_started),
                progress.notStarted,
                MaterialTheme.colorScheme.outline,
            ),
            ProgressSlice(
                stringResource(MR.strings.statistics_in_progress),
                progress.inProgress,
                MaterialTheme.colorScheme.primary,
            ),
            ProgressSlice(
                stringResource(MR.strings.statistics_caught_up),
                progress.caughtUp,
                MaterialTheme.colorScheme.tertiary,
            ),
            ProgressSlice(
                stringResource(MR.strings.completed),
                progress.completed,
                MaterialTheme.colorScheme.secondary,
            ),
        )
        val total = progress.total.coerceAtLeast(1)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(20.dp)
                .semantics {
                    contentDescription = values.joinToString { "${it.label}, ${it.count}" }
                },
        ) {
            values.filter { it.count > 0 }.forEach { slice ->
                Surface(
                    modifier = Modifier.weight(slice.count.toFloat()).height(20.dp),
                    color = slice.color,
                    content = {},
                )
            }
            if (progress.total == 0) {
                Surface(
                    modifier = Modifier.weight(total.toFloat()).height(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    content = {},
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            maxItemsInEachRow = 2,
        ) {
            values.forEach { slice ->
                Row(Modifier.weight(1f).widthIn(min = 150.dp)) {
                    Surface(
                        modifier = Modifier.padding(top = 5.dp).size(10.dp),
                        color = slice.color,
                        shape = CircleShape,
                        content = {},
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = slice.label,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(slice.count.toString(), style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
internal fun StatisticsTopTitlesCard(
    titles: List<StatsTopTitle>,
    typesById: Map<EntryType, StatsType>,
    formatDuration: (Long) -> String,
    onTitleClick: (Long) -> Unit,
) {
    StatisticsSectionCard(title = stringResource(MR.strings.statistics_top_titles)) {
        val visibleTitles = titles.take(5)
        val maximum = visibleTitles.maxOfOrNull(StatsTopTitle::durationMillis)?.coerceAtLeast(1L) ?: 1L
        visibleTitles.forEach { title ->
            val type = typesById[title.type]
            val color = type?.accent?.color() ?: MaterialTheme.colorScheme.primary
            Row(
                modifier = Modifier.fillMaxWidth().clickable { onTitleClick(title.entryId) },
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = title.title,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.width(12.dp))
                Text(formatDuration(title.durationMillis), style = MaterialTheme.typography.labelLarge)
            }
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { title.durationMillis.toFloat() / maximum.toFloat() },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = color,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
            Spacer(Modifier.height(14.dp))
        }
    }
}

@Composable
internal fun StatisticsSectionCard(
    title: String,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
    showContent: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                if (actionLabel != null && onActionClick != null) {
                    TextButton(onClick = onActionClick) { Text(actionLabel) }
                }
            }
            if (showContent) {
                Spacer(Modifier.height(18.dp))
                content()
            }
        }
    }
}

private data class ProgressSlice(
    val label: String,
    val count: Int,
    val color: Color,
)
