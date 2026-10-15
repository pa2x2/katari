package eu.kanade.presentation.more.stats.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import eu.kanade.presentation.more.stats.ActivityState
import eu.kanade.presentation.more.stats.data.StatsActivityWindow
import kotlinx.coroutines.delay
import mihon.presentation.kit.button.Button
import mihon.presentation.kit.button.ButtonStyle
import mihon.presentation.kit.content.StatisticsMetrics
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.progress.LoadingIndicator
import mihon.presentation.kit.surface.Surface
import mihon.presentation.kit.surface.SurfaceContainer
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Laid over the top of the activity chart while another window loads or after it failed to: a delayed loading note,
 * so quick loads don't flash it, or the failure with a retry.
 */
@Composable
internal fun StatisticsActivityRequestStatus(
    state: ActivityState.Available,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val loadingTarget = state.loadingTarget
    var visibleLoadingTarget by remember(loadingTarget) {
        mutableStateOf<StatsActivityWindow?>(null)
    }
    LaunchedEffect(loadingTarget) {
        if (loadingTarget != null) {
            delay(LOADING_STATUS_DELAY_MILLIS)
            visibleLoadingTarget = loadingTarget
        }
    }

    Box(
        modifier = modifier.fillMaxWidth().height(StatisticsMetrics.chartStatusHeight),
        contentAlignment = Alignment.Center,
    ) {
        visibleLoadingTarget?.let { target ->
            Surface(container = SurfaceContainer.High, shape = KatariTheme.shapes.small) {
                Row(
                    modifier = Modifier
                        .semantics { liveRegion = LiveRegionMode.Polite }
                        .padding(horizontal = KatariTheme.spacing.m, vertical = KatariTheme.spacing.s),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    LoadingIndicator(Modifier.size(KatariTheme.sizes.iconSmall))
                    Text(
                        text = stringResource(
                            MR.strings.statistics_loading_period,
                            formatStatisticsWindow(target),
                        ),
                        modifier = Modifier.padding(start = KatariTheme.spacing.s),
                        role = TextRole.Supporting,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        state.failedTarget?.let { target ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                container = SurfaceContainer.High,
                shape = KatariTheme.shapes.small,
            ) {
                Row(
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(
                            MR.strings.statistics_could_not_load_period,
                            formatStatisticsWindow(target),
                        ),
                        modifier = Modifier.weight(1f).padding(start = KatariTheme.spacing.m),
                        role = TextRole.Supporting,
                        tone = ContentTone.Error,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Button(
                        label = stringResource(MR.strings.action_retry),
                        onClick = onRetry,
                        style = ButtonStyle.Text,
                    )
                }
            }
        }
    }
}

private const val LOADING_STATUS_DELAY_MILLIS = 500L
