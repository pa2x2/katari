package eu.kanade.presentation.more.stats.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import eu.kanade.presentation.more.stats.data.StatsActivityWindow
import eu.kanade.presentation.more.stats.data.StatsRange
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** Moves a finite window by one bucket; Today appears next to the dates once an older window is shown. */
@Composable
internal fun StatisticsWindowNavigation(
    window: StatsActivityWindow,
    olderEnabled: Boolean,
    newerEnabled: Boolean,
    onOlder: () -> Unit,
    onNewer: () -> Unit,
    onToday: () -> Unit,
) {
    val finite = window.range != StatsRange.ALL
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (finite) {
            IconButton(enabled = olderEnabled, onClick = onOlder) {
                Icon(Icons.Outlined.ChevronLeft, stringResource(MR.strings.statistics_older_activity))
            }
        }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = formatStatisticsWindow(window),
                style = MaterialTheme.typography.titleSmall,
                textAlign = TextAlign.Center,
            )
            if (finite && !window.isLatest) {
                TextButton(onClick = onToday) { Text(stringResource(MR.strings.statistics_today)) }
            }
        }
        if (finite) {
            IconButton(enabled = newerEnabled, onClick = onNewer) {
                Icon(Icons.Outlined.ChevronRight, stringResource(MR.strings.statistics_newer_activity))
            }
        }
    }
}

/** Period label used when the activity chart is hidden, keeping a way back to the latest window. */
@Composable
internal fun StatisticsPeriodRow(
    period: String,
    showToday: Boolean,
    onToday: () -> Unit,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = period,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (showToday) {
            TextButton(onClick = onToday) { Text(stringResource(MR.strings.statistics_today)) }
        }
    }
}
