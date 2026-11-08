package eu.kanade.tachiyomi.ui.video.player.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextOverflow
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import coil3.compose.AsyncImage
import eu.kanade.tachiyomi.ui.video.player.introtimes.VideoPlayerAiredEpisodesState
import eu.kanade.tachiyomi.ui.video.player.introtimes.VideoPlayerMatchSearchState
import mihon.entry.interactions.anime.introtimes.IntroTimesEpisode
import mihon.entry.interactions.anime.introtimes.theintrodb.AiredEpisode
import mihon.entry.interactions.anime.introtimes.theintrodb.SeasonEpisode
import mihon.entry.interactions.anime.introtimes.theintrodb.TheIntroDbCandidate
import mihon.entry.interactions.anime.introtimes.theintrodb.guessPlacement
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Block
import mihon.presentation.kit.button.Button
import mihon.presentation.kit.button.ButtonSize
import mihon.presentation.kit.button.ButtonStyle
import mihon.presentation.kit.chip.FilterChip
import mihon.presentation.kit.content.CoverBacking
import mihon.presentation.kit.content.CoverFrame
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.content.coverFrame
import mihon.presentation.kit.content.rememberArtworkPlaceholder
import mihon.presentation.kit.feedback.LoadingScreen
import mihon.presentation.kit.feedback.NoResults
import mihon.presentation.kit.field.SearchField
import mihon.presentation.kit.layout.RowList
import mihon.presentation.kit.layout.RowListScope
import mihon.presentation.kit.list.ChipsRow
import mihon.presentation.kit.list.CoverRow
import mihon.presentation.kit.list.RowGroupAction
import mihon.presentation.kit.list.RowGroupGap
import mihon.presentation.kit.list.RowGroupHeader
import mihon.presentation.kit.list.RowGroupItem
import mihon.presentation.kit.list.RowGroupPosition
import mihon.presentation.kit.list.SettingsRow
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import tachiyomi.domain.entry.model.TheIntroDbMedia
import tachiyomi.domain.entry.model.TheIntroDbPlacement
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * The page of the settings where the series or film an episode's TheIntroDB times come from is picked. It searches
 * Wikidata and TVmaze, starting with the title; a film is picked at once, while a series opens its episodes so the
 * viewer can point at the one playing, which places a season split in parts. [current] is what is in use, and
 * [pickedNone] marks None as in use. Picking saves at once and returns to the settings.
 */
@Composable
internal fun VideoPlayerTheIntroDbPage(
    frame: VideoPlayerSettingsFrame,
    episode: IntroTimesEpisode,
    current: TheIntroDbMedia?,
    pickedNone: Boolean,
    search: VideoPlayerMatchSearchState<TheIntroDbCandidate>,
    onSearch: (query: String, immediately: Boolean) -> Unit,
    episodes: VideoPlayerAiredEpisodesState,
    onLoadEpisodes: (TheIntroDbCandidate) -> Unit,
    onPick: (TheIntroDbMedia?) -> Unit,
    onBack: () -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by rememberSaveable { mutableStateOf(episode.scope.title) }
    var series by remember { mutableStateOf<TheIntroDbCandidate?>(null) }
    LaunchedEffect(Unit) { onSearch(query, true) }
    NavigationBackHandler(state = rememberNavigationEventState(NavigationEventInfo.None)) {
        if (series != null) series = null else onBack()
    }
    val pick: (TheIntroDbMedia?) -> Unit = { media ->
        onPick(media)
        onBack()
    }
    val opened = series
    if (opened != null) {
        EpisodesPage(
            frame = frame,
            series = opened,
            episode = episode,
            current = current?.takeIf(opened::isSameAs),
            episodes = episodes,
            onRetry = { onLoadEpisodes(opened) },
            onPick = { placement -> pick(opened.toMedia(placement)) },
            onBack = { series = null },
            onDismissRequest = onDismissRequest,
            modifier = modifier,
        )
        return
    }
    Column(modifier = modifier) {
        VideoPlayerSettingsHeader(
            frame = frame,
            onDismissRequest = onDismissRequest,
            title = stringResource(
                MR.strings.anime_intro_times_series,
                stringResource(MR.strings.intro_times_service_theintrodb),
            ),
            onBack = onBack,
        )
        SearchField(
            query = query,
            onQueryChange = {
                query = it
                onSearch(it, false)
            },
            placeholder = stringResource(MR.strings.anime_intro_times_media_search_placeholder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = KatariTheme.spacing.m, vertical = KatariTheme.spacing.s),
            onSearch = { onSearch(query, true) },
        )
        PageList(frame) {
            // First, so it's in reach whatever the search finds.
            item(key = NONE_ITEM) {
                RowGroupItem(position = RowGroupPosition.of(0, 1), selected = pickedNone) {
                    SettingsRow(
                        title = stringResource(MR.strings.none),
                        subtitle = stringResource(MR.strings.anime_intro_times_series_none_summary),
                        icon = MaterialSymbols.Rounded.Block,
                        onClick = { pick(null) },
                    )
                }
            }
            item(key = NONE_GAP_ITEM) { RowGroupGap() }
            when (search) {
                VideoPlayerMatchSearchState.Searching -> item(key = STATUS_ITEM) {
                    LoadingScreen(Modifier.padding(vertical = KatariTheme.spacing.xl))
                }
                VideoPlayerMatchSearchState.Failed -> item(key = STATUS_ITEM) {
                    FailedRow(stringResource(MR.strings.anime_intro_times_media_search_failed)) {
                        onSearch(query, true)
                    }
                }
                is VideoPlayerMatchSearchState.Results -> if (search.found.isEmpty()) {
                    item(key = STATUS_ITEM) { NoResults() }
                } else {
                    itemsIndexed(search.found, key = { index, _ -> "candidate_$index" }) { index, candidate ->
                        RowGroupItem(
                            position = RowGroupPosition.of(index, search.found.size),
                            selected = current != null && candidate.isSameAs(current),
                        ) {
                            CandidateRow(candidate) {
                                if (candidate.film) {
                                    pick(candidate.toMedia(placement = null))
                                } else {
                                    onLoadEpisodes(candidate)
                                    series = candidate
                                }
                            }
                        }
                    }
                }
            }
            tvMazeCredit()
        }
    }
}

/**
 * A series' episodes, a season at a time, opened at the one thought to be playing: the one [current] places it at, or
 * else the guess an automatic match would make. Tapping one places the scope so that it is the playing episode.
 */
@Composable
private fun EpisodesPage(
    frame: VideoPlayerSettingsFrame,
    series: TheIntroDbCandidate,
    episode: IntroTimesEpisode,
    current: TheIntroDbMedia?,
    episodes: VideoPlayerAiredEpisodesState,
    onRetry: () -> Unit,
    onPick: (TheIntroDbPlacement) -> Unit,
    onBack: () -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // An episode without a number of its own is placed as the scope's first.
    val playing = episode.number ?: 1
    Column(modifier = modifier) {
        VideoPlayerSettingsHeader(
            frame = frame,
            onDismissRequest = onDismissRequest,
            title = series.title,
            onBack = onBack,
        )
        val aired = (episodes as? VideoPlayerAiredEpisodesState.Loaded)?.episodes
        if (aired != null && aired.isEmpty()) {
            // TVmaze doesn't list the series, so there is nothing to point at: it is placed as an automatic match is.
            LaunchedEffect(series) { onPick(current?.placement ?: guessPlacement(episode.scope, aired)) }
        }
        val placement = remember(aired, current) {
            aired?.let { current?.placement ?: guessPlacement(episode.scope, it) }
        }
        val playingAired = placement?.let { SeasonEpisode(it.season, it.firstEpisode + playing - 1) }
        var season by remember(playingAired) { mutableStateOf(playingAired?.season) }
        PageList(frame) {
            when {
                episodes == VideoPlayerAiredEpisodesState.Loading -> item(key = STATUS_ITEM) {
                    LoadingScreen(Modifier.padding(vertical = KatariTheme.spacing.xl))
                }
                episodes == VideoPlayerAiredEpisodesState.Failed -> item(key = STATUS_ITEM) {
                    FailedRow(stringResource(MR.strings.anime_intro_times_episodes_failed), onRetry)
                }
                aired != null -> {
                    val seasons = aired.map { it.season }.distinct()
                    item(key = SEASONS_ITEM) {
                        ChipsRow(title = stringResource(MR.strings.anime_intro_times_season)) {
                            seasons.forEach { number ->
                                FilterChip(
                                    label = number.toString(),
                                    selected = number == season,
                                    onClick = { season = number },
                                )
                            }
                        }
                    }
                    item(key = WHICH_ITEM) {
                        RowGroupHeader(stringResource(MR.strings.anime_intro_times_which_episode))
                    }
                    val shown = aired.filter { it.season == (season ?: seasons.first()) }
                    itemsIndexed(shown, key = { _, listed ->
                        "episode_${listed.season}_${listed.number}"
                    }) { index, listed ->
                        RowGroupItem(
                            position = RowGroupPosition.of(index, shown.size),
                            selected = playingAired?.season == listed.season &&
                                playingAired.episode == listed.number,
                        ) {
                            AiredEpisodeRow(listed) {
                                onPick(TheIntroDbPlacement(listed.season, listed.number - (playing - 1)))
                            }
                        }
                    }
                }
            }
            tvMazeCredit()
        }
    }
}

@Composable
private fun ColumnScope.PageList(frame: VideoPlayerSettingsFrame, content: RowListScope.() -> Unit) {
    RowList(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f, fill = frame == VideoPlayerSettingsFrame.Panel),
        contentPadding = PaddingValues(bottom = KatariTheme.spacing.l),
        content = content,
    )
}

// TVmaze shares its data under CC BY-SA, which asks for a credit with a link wherever it shows.
private fun RowListScope.tvMazeCredit() {
    item(key = CREDIT_ITEM) {
        val uriHandler = LocalUriHandler.current
        RowGroupAction(
            label = stringResource(MR.strings.anime_intro_times_tvmaze_credit),
            onClick = { uriHandler.openUri(TVMAZE_URL) },
        )
    }
}

@Composable
private fun CandidateRow(candidate: TheIntroDbCandidate, onClick: () -> Unit) {
    CoverRow(
        cover = {
            AsyncImage(
                model = candidate.coverUrl,
                contentDescription = null,
                placeholder = rememberArtworkPlaceholder(),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .coverFrame(CoverFrame.Thumbnail, CoverBacking.Placeholder),
            )
        },
        onClick = onClick,
    ) {
        Text(candidate.title, role = TextRole.ItemTitle, maxLines = 2, overflow = TextOverflow.Ellipsis)
        val kind = stringResource(
            if (candidate.film) MR.strings.anime_intro_times_kind_film else MR.strings.anime_intro_times_kind_series,
        )
        Text(
            listOfNotNull(kind, candidate.year?.toString()).joinToString(" · "),
            role = TextRole.Supporting,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun AiredEpisodeRow(episode: AiredEpisode, onClick: () -> Unit) {
    val dateFormat = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) }
    SettingsRow(
        title = episode.name,
        subtitle = listOfNotNull(
            stringResource(MR.strings.anime_intro_times_episode_number, episode.number),
            episode.aired?.format(dateFormat),
        ).joinToString(" · "),
        onClick = onClick,
    )
}

@Composable
private fun FailedRow(title: String, onRetry: () -> Unit) {
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

private const val TVMAZE_URL = "https://www.tvmaze.com"
private const val STATUS_ITEM = "status"
private const val NONE_ITEM = "none"
private const val NONE_GAP_ITEM = "none-gap"
private const val SEASONS_ITEM = "seasons"
private const val WHICH_ITEM = "which"
private const val CREDIT_ITEM = "credit"
