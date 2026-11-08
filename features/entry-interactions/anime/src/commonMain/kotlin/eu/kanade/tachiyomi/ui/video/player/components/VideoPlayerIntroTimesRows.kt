package eu.kanade.tachiyomi.ui.video.player.components

import androidx.compose.runtime.Composable
import eu.kanade.tachiyomi.source.entry.PlaybackMarker
import eu.kanade.tachiyomi.ui.video.player.introtimes.VideoPlayerIntroTimesState
import eu.kanade.tachiyomi.ui.video.player.introtimes.VideoPlayerServiceTimes
import eu.kanade.tachiyomi.ui.video.player.introtimes.label
import eu.kanade.tachiyomi.ui.video.player.introtimes.markSource
import eu.kanade.tachiyomi.ui.video.player.markers.VideoPlayerChapterName
import mihon.entry.interactions.anime.introtimes.IntroTimesResult
import mihon.entry.interactions.anime.introtimes.IntroTimesService
import mihon.entry.interactions.anime.introtimes.OnlineMarker
import mihon.presentation.kit.button.Button
import mihon.presentation.kit.button.ButtonSize
import mihon.presentation.kit.button.ButtonStyle
import mihon.presentation.kit.control.MediaMarkSwatch
import mihon.presentation.kit.list.SettingsRow
import mihon.presentation.kit.progress.CircularProgressIndicator
import mihon.presentation.kit.progress.ProgressSize
import mihon.presentation.kit.surface.StatusPill
import mihon.presentation.kit.surface.StatusTone
import tachiyomi.domain.entry.model.IntroTimesSeries
import tachiyomi.domain.entry.model.TheIntroDbMedia
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Where the playing episode's times stand with each service turned on, under the skip choice: a row per service with
 * the colour its spans take on the timeline, whether its times are in use, what it matched, and the action that fixes
 * it.
 */
@Composable
internal fun VideoPlayerIntroTimesRows(
    state: VideoPlayerIntroTimesState,
    onChange: (IntroTimesService) -> Unit,
    onRetry: (IntroTimesService) -> Unit,
) {
    state.aniSkip?.let { times ->
        ServiceRow(IntroTimesService.ANISKIP, times, state.markers, { aniSkipMatch(it) }, onChange, onRetry)
    }
    state.theIntroDb?.let { times ->
        ServiceRow(IntroTimesService.THEINTRODB, times, state.markers, { theIntroDbMatch(it) }, onChange, onRetry)
    }
}

@Composable
private fun <M> ServiceRow(
    service: IntroTimesService,
    times: VideoPlayerServiceTimes<M>,
    used: List<OnlineMarker>,
    describe: @Composable (M) -> String,
    onChange: (IntroTimesService) -> Unit,
    onRetry: (IntroTimesService) -> Unit,
) {
    val swatch: @Composable () -> Unit = { MediaMarkSwatch(service.markSource) }
    val result = (times as? VideoPlayerServiceTimes.Done)?.result
    if (result == null) {
        SettingsRow(
            title = stringResource(service.label),
            subtitle = stringResource(MR.strings.anime_intro_times_looking),
            leading = swatch,
            trailing = { CircularProgressIndicator(size = ProgressSize.Inline) },
        )
        return
    }
    val change: @Composable () -> Unit = {
        RowAction(stringResource(MR.strings.anime_intro_times_change)) { onChange(service) }
    }
    val inUse = used.any { it.service == service }
    SettingsRow(
        title = stringResource(service.label),
        subtitle = when (result) {
            is IntroTimesResult.Found -> lines(foundLine(service, result.markers, used), describe(result.match))
            is IntroTimesResult.Unnumbered -> lines(
                stringResource(MR.strings.anime_intro_times_none_for_unnumbered),
                describe(result.match),
            )
            is IntroTimesResult.NoMatch -> lines(
                stringResource(MR.strings.anime_intro_times_no_series),
                stringResource(MR.strings.anime_intro_times_searched_for, result.title),
            )
            IntroTimesResult.TurnedOff -> lines(
                stringResource(MR.strings.anime_intro_times_turned_off),
                stringResource(MR.strings.anime_intro_times_turned_off_summary),
            )
            IntroTimesResult.Unreachable -> lines(
                stringResource(MR.strings.anime_intro_times_unreachable),
                stringResource(MR.strings.anime_intro_times_unreachable_summary),
            )
        },
        leading = swatch,
        titleSuffix = if (inUse) {
            { StatusPill(stringResource(MR.strings.anime_intro_times_in_use), StatusTone.Ready) }
        } else {
            null
        },
        trailing = when (result) {
            is IntroTimesResult.NoMatch -> {
                { RowAction(stringResource(MR.strings.anime_intro_times_find)) { onChange(service) } }
            }
            IntroTimesResult.Unreachable -> {
                { RowAction(stringResource(MR.strings.action_retry)) { onRetry(service) } }
            }
            else -> change
        },
    )
}

/** What a service's times for the episode come to: all of them used, some, or none because another's win. */
@Composable
private fun foundLine(service: IntroTimesService, found: List<PlaybackMarker>, used: List<OnlineMarker>): String {
    if (found.isEmpty()) return stringResource(MR.strings.anime_intro_times_none_for_episode)
    val own = used.filter { it.service == service }
    return when {
        own.size == found.size -> stringResource(MR.strings.anime_intro_times_found)
        own.isNotEmpty() -> stringResource(
            MR.strings.anime_intro_times_partly_used,
            own.map { it.marker.kind }.distinct().map { VideoPlayerChapterName.Marker(it).text() }.joinToString(),
        )
        else -> stringResource(
            MR.strings.anime_intro_times_used_instead,
            stringResource((used.firstOrNull()?.service ?: service).label),
        )
    }
}

@Composable
private fun aniSkipMatch(series: IntroTimesSeries): String = matchedTo(series.title, series.year)

@Composable
private fun theIntroDbMatch(media: TheIntroDbMedia): String {
    val matched = matchedTo(media.title, media.year)
    val placement = media.placement ?: return matched
    return listOf(
        matched,
        stringResource(MR.strings.anime_intro_times_season_from, placement.season, placement.firstEpisode),
    ).joinToString(" · ")
}

@Composable
private fun matchedTo(title: String, year: Int?): String =
    year?.let { stringResource(MR.strings.anime_intro_times_matched, title, it) }
        ?: stringResource(MR.strings.anime_intro_times_matched_no_year, title)

private fun lines(vararg lines: String): String = lines.joinToString("\n")

@Composable
private fun RowAction(label: String, onClick: () -> Unit) {
    Button(label = label, onClick = onClick, style = ButtonStyle.Text, size = ButtonSize.Small)
}
