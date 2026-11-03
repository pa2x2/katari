package eu.kanade.presentation.more.stats.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import eu.kanade.presentation.more.stats.ActivityState
import mihon.presentation.kit.button.Button
import mihon.presentation.kit.button.ButtonStyle
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.progress.LoadingIndicator
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import tachiyomi.i18n.*
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = KatariTheme.spacing.l, vertical = KatariTheme.spacing.s)
            .semantics { liveRegion = LiveRegionMode.Polite },
        horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.m),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (loading != null) LoadingIndicator(Modifier.size(KatariTheme.sizes.iconSmall))
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
            role = TextRole.Supporting,
        )
        if (loading == null) {
            Button(label = stringResource(MR.strings.action_retry), onClick = onRetry, style = ButtonStyle.Text)
        }
    }
}
