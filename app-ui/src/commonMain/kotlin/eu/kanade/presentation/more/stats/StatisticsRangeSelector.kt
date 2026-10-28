package eu.kanade.presentation.more.stats

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import eu.kanade.presentation.more.stats.data.StatsRange
import mihon.presentation.kit.control.SegmentedButtons
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun StatisticsRangeSelector(selected: StatsRange, onSelected: (StatsRange) -> Unit) {
    SegmentedButtons(
        options = StatsRange.entries,
        selected = selected,
        onSelect = onSelected,
        label = { range ->
            stringResource(
                when (range) {
                    StatsRange.SEVEN_DAYS -> MR.strings.statistics_range_7_days
                    StatsRange.THIRTY_DAYS -> MR.strings.statistics_range_30_days
                    StatsRange.ONE_YEAR -> MR.strings.statistics_range_1_year
                    StatsRange.ALL -> MR.strings.all
                },
            )
        },
        modifier = Modifier.fillMaxWidth(),
    )
}
