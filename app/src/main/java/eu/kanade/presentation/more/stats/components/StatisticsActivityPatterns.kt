package eu.kanade.presentation.more.stats.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

@Composable
@OptIn(ExperimentalLayoutApi::class)
internal fun StatisticsActivityPatternsCard(
    sessionCount: Long?,
    averageSessionDurationMillis: Long?,
    longestSessionDurationMillis: Long?,
    activeDays: Int?,
    formatDuration: (Long) -> String,
) {
    val hasSessions = sessionCount?.let { it > 0L } == true
    StatisticsSectionCard(
        title = stringResource(MR.strings.statistics_activity_patterns),
    ) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            maxItemsInEachRow = 3,
        ) {
            StatisticsInsightTile(
                value = averageSessionDurationMillis
                    ?.takeIf { hasSessions }
                    ?.let(formatDuration) ?: "—",
                label = stringResource(MR.strings.statistics_average_visit),
                modifier = Modifier.weight(1f).widthIn(min = 96.dp).fillMaxRowHeight(),
            )
            StatisticsInsightTile(
                value = longestSessionDurationMillis
                    ?.takeIf { hasSessions }
                    ?.let(formatDuration) ?: "—",
                label = stringResource(MR.strings.statistics_longest_visit),
                modifier = Modifier.weight(1f).widthIn(min = 96.dp).fillMaxRowHeight(),
            )
            StatisticsInsightTile(
                value = activeDays?.toString() ?: "—",
                label = stringResource(MR.strings.statistics_active_days),
                modifier = Modifier.weight(1f).widthIn(min = 96.dp).fillMaxRowHeight(),
            )
        }
    }
}
