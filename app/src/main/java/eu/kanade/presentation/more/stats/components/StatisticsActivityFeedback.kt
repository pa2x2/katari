package eu.kanade.presentation.more.stats.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.more.stats.ActivityState
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

/** Retains loading and retry feedback when the user hides the activity chart. */
@Composable
internal fun StatisticsActivityFeedback(state: ActivityState, onRetry: () -> Unit) {
    val loading = when (state) {
        is ActivityState.Loading -> state.target
        is ActivityState.Available -> state.loadingTarget
        is ActivityState.Failed -> null
    }
    val failed = when (state) {
        is ActivityState.Failed -> state.target
        is ActivityState.Available -> state.failedTarget
        is ActivityState.Loading -> null
    }
    val target = loading ?: failed ?: return
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (loading != null) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
        Text(
            text = stringResource(
                if (loading != null) {
                    MR.strings.statistics_loading_period
                } else {
                    MR.strings.statistics_could_not_load_period
                },
                formatStatisticsWindow(target),
            ),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
        )
        if (loading == null) {
            TextButton(onClick = onRetry) { Text(stringResource(MR.strings.action_retry)) }
        }
    }
}
