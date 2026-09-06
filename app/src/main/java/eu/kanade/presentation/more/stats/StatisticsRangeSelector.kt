package eu.kanade.presentation.more.stats

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import eu.kanade.presentation.more.stats.data.StatsRange
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun StatisticsRangeSelector(selected: StatsRange, onSelected: (StatsRange) -> Unit) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        StatsRange.entries.forEachIndexed { index, range ->
            SegmentedButton(
                selected = selected == range,
                onClick = { onSelected(range) },
                shape = SegmentedButtonDefaults.itemShape(index, StatsRange.entries.size),
                label = {
                    Text(
                        stringResource(
                            when (range) {
                                StatsRange.SEVEN_DAYS -> MR.strings.statistics_range_7_days
                                StatsRange.THIRTY_DAYS -> MR.strings.statistics_range_30_days
                                StatsRange.ONE_YEAR -> MR.strings.statistics_range_1_year
                                StatsRange.ALL -> MR.strings.all
                            },
                        ),
                    )
                },
            )
        }
    }
}
