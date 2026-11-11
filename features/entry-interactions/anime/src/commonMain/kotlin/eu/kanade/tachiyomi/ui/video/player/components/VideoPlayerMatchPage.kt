package eu.kanade.tachiyomi.ui.video.player.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import app.katari.entry.interactions.anime.introtimes.IntroTimesService
import app.katari.entry.interactions.anime.introtimes.seriesTitle
import app.katari.presentation.kit.button.Button
import app.katari.presentation.kit.button.ButtonSize
import app.katari.presentation.kit.button.ButtonStyle
import app.katari.presentation.kit.layout.RowList
import app.katari.presentation.kit.layout.RowListScope
import app.katari.presentation.kit.list.SettingsRow
import app.katari.presentation.kit.theme.KatariTheme
import eu.kanade.tachiyomi.ui.video.player.introtimes.VideoPlayerIntroTimesMatch
import eu.kanade.tachiyomi.ui.video.player.introtimes.label
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * What a match page says in a service's own words, and how it shows what the service's search finds.
 *
 * @property searchFailed said when the search can't be reached, and [openFailed] when a series' episodes can't.
 * @property inUse whether a found candidate is the match in use.
 * @property credit rows that credit where the page's data comes from, at its end.
 */
internal class VideoPlayerMatchPageContent<C, M>(
    val searchPlaceholder: String,
    val searchFailed: String,
    val openFailed: String,
    val inUse: (candidate: C, match: M) -> Boolean,
    val candidateRow: @Composable (candidate: C, onClick: () -> Unit) -> Unit,
    val credit: RowListScope.() -> Unit = {},
)

/**
 * The page of the settings where what an episode's times from [service] come from is changed. It opens on the match in
 * use, with the playing episode where that match places it, and on the search when nothing is in use; a series or film
 * picked in the search opens the same way. Nothing is kept until [onUse] is called with what the viewer settled on,
 * null for None, which returns to the settings.
 */
@Composable
internal fun <C, M : Any> VideoPlayerMatchPage(
    frame: VideoPlayerSettingsFrame,
    service: IntroTimesService,
    match: VideoPlayerIntroTimesMatch<C, M>,
    content: VideoPlayerMatchPageContent<C, M>,
    onUse: (M?) -> Unit,
    onBack: () -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state = match.state.collectAsState().value ?: return
    // Back from the search returns to the series it was opened from, when there is one.
    val leave = { if (!match.leaveSearch()) onBack() }
    NavigationBackHandler(state = rememberNavigationEventState(NavigationEventInfo.None), onBackCompleted = leave)
    Column(modifier = modifier) {
        VideoPlayerSettingsHeader(
            frame = frame,
            onDismissRequest = onDismissRequest,
            title = stringResource(MR.strings.anime_intro_times_series, stringResource(service.label)),
            onBack = leave,
        )
        val opened = state.opened
        if (state.searching || opened == null) {
            VideoPlayerMatchSearchList(
                frame = frame,
                initialQuery = state.episode.scope.seriesTitle,
                search = match.search.state.collectAsState().value,
                onSearch = match.search::search,
                pickedNone = state.pickedNone,
                selected = { candidate -> state.current?.let { content.inUse(candidate, it) } == true },
                content = content,
                onPickNone = { onUse(null) },
                onPick = match::openCandidate,
            )
        } else {
            VideoPlayerMatchEpisodePick(
                frame = frame,
                service = service,
                state = state,
                opened = opened,
                content = content,
                onSearch = match::showSearch,
                onShowSeason = match::showSeason,
                onPick = match::pick,
                onRetry = match::retry,
                onUse = onUse,
            )
        }
    }
}

/** The rows of a match page under its header, filling a panel's height and as tall as its rows in a sheet. */
@Composable
internal fun ColumnScope.VideoPlayerMatchList(frame: VideoPlayerSettingsFrame, content: RowListScope.() -> Unit) {
    RowList(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f, fill = frame == VideoPlayerSettingsFrame.Panel),
        contentPadding = PaddingValues(bottom = KatariTheme.spacing.l),
        content = content,
    )
}

/** A row saying what couldn't be reached, with the button that tries again. */
@Composable
internal fun VideoPlayerMatchFailedRow(title: String, onRetry: () -> Unit) {
    SettingsRow(
        title = title,
        trailing = {
            Button(
                label = stringResource(MR.strings.action_retry),
                onClick = onRetry,
                style = ButtonStyle.Text,
                size = ButtonSize.Small,
            )
        },
    )
}
