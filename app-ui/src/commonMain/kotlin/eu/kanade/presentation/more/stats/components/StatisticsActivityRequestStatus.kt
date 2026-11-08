package eu.kanade.presentation.more.stats.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import app.katari.presentation.kit.button.Button
import app.katari.presentation.kit.button.ButtonStyle
import app.katari.presentation.kit.content.StatisticsMetrics
import app.katari.presentation.kit.content.Text
import app.katari.presentation.kit.progress.LoadingIndicator
import app.katari.presentation.kit.surface.Card
import app.katari.presentation.kit.surface.SurfaceContainer
import app.katari.presentation.kit.theme.ContentTone
import app.katari.presentation.kit.theme.KatariTheme
import app.katari.presentation.kit.theme.TextRole
import eu.kanade.presentation.more.stats.ActivityState
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Laid over the top of the activity chart while another window loads or after it failed to: a loading note, or the
 * failure with a retry.
 */
@Composable
internal fun StatisticsActivityRequestStatus(
    state: ActivityState.Available,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxWidth().height(StatisticsMetrics.chartStatusHeight),
        contentAlignment = Alignment.Center,
    ) {
        state.loadingTarget?.let { target ->
            Card(container = SurfaceContainer.High) {
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
            Card(modifier = Modifier.fillMaxWidth(), container = SurfaceContainer.High) {
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
