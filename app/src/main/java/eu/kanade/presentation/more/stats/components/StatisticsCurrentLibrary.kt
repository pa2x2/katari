package eu.kanade.presentation.more.stats.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.more.stats.data.StatsProgress
import eu.kanade.presentation.more.stats.data.StatsType
import eu.kanade.tachiyomi.source.entry.EntryType
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun StatisticsCurrentLibraryHeader(titleCount: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(MR.strings.statistics_library),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = pluralStringResource(MR.plurals.statistics_title_count, titleCount, titleCount),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
internal fun StatisticsLibraryCard(
    titleCounts: Map<EntryType, Int>,
    types: List<StatsType>,
    onTypeClick: (EntryType) -> Unit,
) {
    StatisticsSectionCard(title = stringResource(MR.strings.statistics_by_media)) {
        types.forEachIndexed { index, type ->
            val count = titleCounts[type.type] ?: 0
            val label = stringResource(type.displayName)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onTypeClick(type.type) }
                    .semantics { contentDescription = "$label, $count" }
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    modifier = Modifier.size(10.dp),
                    color = type.accent.color(),
                    shape = CircleShape,
                    content = {},
                )
                Spacer(Modifier.width(10.dp))
                Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                Text(count.toString(), style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (index != types.lastIndex) HorizontalDivider()
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
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
            ProgressRow(stringResource(MR.strings.statistics_not_started), progress.notStarted),
            ProgressRow(stringResource(MR.strings.statistics_in_progress), progress.inProgress),
            ProgressRow(stringResource(MR.strings.statistics_caught_up), progress.caughtUp),
            ProgressRow(stringResource(MR.strings.completed), progress.completed),
        )
        val total = progress.total.coerceAtLeast(1)
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            maxItemsInEachRow = 2,
        ) {
            values.forEach { row ->
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .widthIn(min = 140.dp)
                        .semantics { contentDescription = "${row.label}, ${row.count}" },
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = row.label,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(row.count.toString(), style = MaterialTheme.typography.labelLarge)
                        }
                        Spacer(Modifier.height(10.dp))
                        Surface(
                            modifier = Modifier.fillMaxWidth().height(3.dp),
                            color = MaterialTheme.colorScheme.surface,
                        ) {
                            Row {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth(row.count.toFloat() / total)
                                        .height(3.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    content = {},
                                )
                            }
                        }
                    }
                }
            }
        }
        if (progress.isPartial) {
            Text(
                text = stringResource(
                    MR.strings.statistics_progress_coverage,
                    progress.total,
                    progress.libraryTotal,
                ),
                modifier = Modifier.padding(top = 10.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private data class ProgressRow(
    val label: String,
    val count: Int,
)
