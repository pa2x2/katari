package eu.kanade.tachiyomi.ui.video.player.components

import androidx.compose.runtime.Composable
import eu.kanade.tachiyomi.ui.video.player.introtimes.VideoPlayerIntroTimesState
import mihon.entry.interactions.anime.introtimes.IntroTimesResult
import mihon.presentation.kit.button.Button
import mihon.presentation.kit.button.ButtonSize
import mihon.presentation.kit.button.ButtonStyle
import mihon.presentation.kit.list.SettingsRow
import mihon.presentation.kit.progress.CircularProgressIndicator
import mihon.presentation.kit.progress.ProgressSize
import tachiyomi.domain.entry.model.IntroTimesSeries
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** Where the playing episode's times from AniSkip stand, under the skip choice, with the action that fixes them. */
@Composable
internal fun VideoPlayerIntroTimesRow(
    state: VideoPlayerIntroTimesState,
    onChangeSeries: () -> Unit,
    onRetry: () -> Unit,
) {
    val change: @Composable () -> Unit = {
        RowAction(stringResource(MR.strings.anime_intro_times_change), onChangeSeries)
    }
    when (val result = state.result) {
        null -> SettingsRow(
            title = stringResource(MR.strings.anime_intro_times_looking),
            leading = { CircularProgressIndicator(size = ProgressSize.Inline) },
        )
        is IntroTimesResult.Matched -> SettingsRow(
            title = when {
                result.markers.isNotEmpty() -> stringResource(MR.strings.anime_intro_times_found)
                result.episodeNumber != null -> stringResource(MR.strings.anime_intro_times_none_for_episode)
                else -> stringResource(MR.strings.anime_intro_times_none_for_unnumbered)
            },
            subtitle = matchedTo(result.series),
            trailing = change,
        )
        is IntroTimesResult.NoSeries -> SettingsRow(
            title = stringResource(MR.strings.anime_intro_times_no_series),
            subtitle = stringResource(MR.strings.anime_intro_times_searched_for, result.title),
            trailing = { RowAction(stringResource(MR.strings.anime_intro_times_find), onChangeSeries) },
        )
        IntroTimesResult.TurnedOff -> SettingsRow(
            title = stringResource(MR.strings.anime_intro_times_turned_off),
            subtitle = stringResource(MR.strings.anime_intro_times_turned_off_summary),
            trailing = change,
        )
        IntroTimesResult.Unreachable -> SettingsRow(
            title = stringResource(MR.strings.anime_intro_times_unreachable),
            subtitle = stringResource(MR.strings.anime_intro_times_unreachable_summary),
            trailing = { RowAction(stringResource(MR.strings.action_retry), onRetry) },
        )
    }
}

@Composable
private fun matchedTo(series: IntroTimesSeries): String =
    series.year?.let { stringResource(MR.strings.anime_intro_times_matched, series.title, it) }
        ?: stringResource(MR.strings.anime_intro_times_matched_no_year, series.title)

@Composable
private fun RowAction(label: String, onClick: () -> Unit) {
    Button(label = label, onClick = onClick, style = ButtonStyle.Text, size = ButtonSize.Small)
}
