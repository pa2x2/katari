package eu.kanade.presentation.more.stats.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.more.stats.data.StatsLibraryInsights
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

@Composable
@OptIn(ExperimentalLayoutApi::class)
internal fun StatisticsLibraryInsightsCard(library: StatsLibraryInsights) {
    StatisticsSectionCard(
        title = stringResource(MR.strings.statistics_library_insights),
    ) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            maxItemsInEachRow = 2,
        ) {
            library.topGenre?.let { genre ->
                StatisticsInsightTile(
                    value = genre,
                    label = stringResource(MR.strings.statistics_top_genre),
                    modifier = Modifier.weight(1f).widthIn(min = 140.dp).fillMaxRowHeight(),
                )
            }
            StatisticsInsightTile(
                value = library.categoryCount.toString(),
                label = stringResource(MR.strings.categories),
                modifier = Modifier.weight(1f).widthIn(min = 140.dp).fillMaxRowHeight(),
            )
        }
    }
}
