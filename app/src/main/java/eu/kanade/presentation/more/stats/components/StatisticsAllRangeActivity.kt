package eu.kanade.presentation.more.stats.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
internal fun AllRangeSummary(
    totalDuration: String,
    trackingStartDate: LocalDate?,
    year: Int?,
) {
    Column {
        Text(
            text = totalDuration,
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            text = if (year == null) {
                stringResource(MR.strings.statistics_total_recorded_time)
            } else {
                stringResource(MR.strings.statistics_recorded_in_year, year)
            },
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
        if (year == null && trackingStartDate != null) {
            val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
            Text(
                text = stringResource(
                    MR.strings.statistics_precisely_dated_since,
                    trackingStartDate.format(formatter),
                ),
                modifier = Modifier.padding(top = 4.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Composable
internal fun AllRangeYearNavigation(
    year: Int,
    previousEnabled: Boolean,
    nextEnabled: Boolean,
    onAllActivity: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = onAllActivity) {
            Icon(Icons.Outlined.ChevronLeft, contentDescription = null)
            Text(stringResource(MR.strings.statistics_all_activity))
        }
        IconButton(enabled = previousEnabled, onClick = onPrevious) {
            Icon(Icons.Outlined.ChevronLeft, stringResource(MR.strings.statistics_previous_year))
        }
        Text(
            text = year.toString(),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleSmall,
            textAlign = TextAlign.Center,
        )
        IconButton(enabled = nextEnabled, onClick = onNext) {
            Icon(Icons.Outlined.ChevronRight, stringResource(MR.strings.statistics_next_year))
        }
    }
}

@Composable
internal fun AllRangeEmptyActivity() {
    Column(
        modifier = Modifier.fillMaxWidth().height(140.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(MR.strings.statistics_no_activity),
            style = MaterialTheme.typography.titleSmall,
        )
        Text(
            text = stringResource(MR.strings.statistics_all_activity_empty_hint),
            modifier = Modifier.padding(top = 6.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
