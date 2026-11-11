package eu.kanade.tachiyomi.ui.video.player.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import app.katari.entry.interactions.anime.introtimes.IntroTimesEpisode
import app.katari.entry.interactions.anime.introtimes.IntroTimesMatchEpisode
import app.katari.entry.interactions.anime.introtimes.IntroTimesMatchPick
import app.katari.entry.interactions.anime.introtimes.IntroTimesMatchSeason
import app.katari.entry.interactions.anime.introtimes.IntroTimesService
import app.katari.presentation.kit.button.Button
import app.katari.presentation.kit.button.ButtonSize
import app.katari.presentation.kit.button.ButtonStyle
import app.katari.presentation.kit.chip.FilterChip
import app.katari.presentation.kit.feedback.LoadingScreen
import app.katari.presentation.kit.field.TextField
import app.katari.presentation.kit.layout.RowListScope
import app.katari.presentation.kit.list.ChipsRow
import app.katari.presentation.kit.list.ControlRow
import app.katari.presentation.kit.list.InfoRow
import app.katari.presentation.kit.list.NumberChoiceRow
import app.katari.presentation.kit.list.RowGroupAction
import app.katari.presentation.kit.list.RowGroupGap
import app.katari.presentation.kit.list.RowGroupItem
import app.katari.presentation.kit.list.RowGroupPosition
import app.katari.presentation.kit.list.SettingsRow
import app.katari.presentation.kit.theme.KatariTheme
import eu.kanade.tachiyomi.ui.video.player.introtimes.VideoPlayerMatchOpened
import eu.kanade.tachiyomi.ui.video.player.introtimes.VideoPlayerMatchState
import eu.kanade.tachiyomi.ui.video.player.introtimes.VideoPlayerMatchTimes
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * A match page on the series or film it has [opened]: what is playing, the series with the way to the search, its
 * seasons, the episodes of the one shown, and what the service has for the episode pointed at, which [onUse] keeps.
 */
@Composable
internal fun <C, M : Any> ColumnScope.VideoPlayerMatchEpisodePick(
    frame: VideoPlayerSettingsFrame,
    service: IntroTimesService,
    state: VideoPlayerMatchState<M>,
    opened: VideoPlayerMatchOpened<M>,
    content: VideoPlayerMatchPageContent<C, M>,
    onSearch: () -> Unit,
    onShowSeason: (Int) -> Unit,
    onPick: (IntroTimesMatchPick) -> Unit,
    onRetry: () -> Unit,
    onUse: (M) -> Unit,
) {
    VideoPlayerMatchList(frame) {
        item(key = PLAYING_ITEM) {
            InfoRow(
                text = stringResource(MR.strings.anime_intro_times_playing, state.episode.playingName),
                showIcon = false,
            )
        }
        when (opened) {
            VideoPlayerMatchOpened.Loading -> item(key = STATUS_ITEM) {
                LoadingScreen(Modifier.padding(vertical = KatariTheme.spacing.xl))
            }
            VideoPlayerMatchOpened.Failed -> {
                item(key = STATUS_ITEM) { VideoPlayerMatchFailedRow(content.openFailed, onRetry) }
                item(key = SEARCH_ITEM) {
                    RowGroupAction(label = stringResource(MR.strings.action_search), onClick = onSearch)
                }
            }
            is VideoPlayerMatchOpened.Loaded -> loaded(
                service = service,
                lengthMs = state.lengthMs,
                loaded = opened,
                onSearch = onSearch,
                onShowSeason = onShowSeason,
                onPick = onPick,
                onRetry = onRetry,
                onUse = onUse,
            )
        }
        content.credit(this)
    }
}

private fun <M : Any> RowListScope.loaded(
    service: IntroTimesService,
    lengthMs: Long,
    loaded: VideoPlayerMatchOpened.Loaded<M>,
    onSearch: () -> Unit,
    onShowSeason: (Int) -> Unit,
    onPick: (IntroTimesMatchPick) -> Unit,
    onRetry: () -> Unit,
    onUse: (M) -> Unit,
) {
    val choice = loaded.choice
    val season = choice.seasons.firstOrNull { it.number == loaded.season }
    item(key = SERIES_ITEM) {
        RowGroupItem(position = RowGroupPosition.of(0, 1)) {
            SettingsRow(
                title = season?.title ?: choice.title,
                subtitle = seriesDetails(year = if (season?.title != null) season.year else choice.year, season),
                trailing = {
                    Button(
                        label = stringResource(MR.strings.action_search),
                        onClick = onSearch,
                        style = ButtonStyle.Text,
                        size = ButtonSize.Small,
                    )
                },
            )
        }
    }
    if (choice.seasons.size > 1) {
        item(key = SEASONS_ITEM) {
            ChipsRow(title = stringResource(MR.strings.anime_intro_times_season)) {
                choice.seasons.forEach { listed ->
                    FilterChip(
                        label = listed.number.toString(),
                        selected = listed.number == loaded.season,
                        onClick = { onShowSeason(listed.number) },
                    )
                }
            }
        }
    }
    val episodes = season?.episodes
    when {
        choice.film -> item(key = EPISODES_ITEM) { RowGroupGap() }
        episodes != null -> item(key = EPISODES_ITEM) {
            val picked = loaded.pick?.takeIf { it.season == season.number }?.episode
            NumberChoiceRow(
                title = stringResource(MR.strings.anime_intro_times_which_episode),
                numbers = remember(episodes) { episodes.map { it.number } },
                selected = picked,
                onSelect = { onPick(IntroTimesMatchPick(season.number, it)) },
                jumpLabel = stringResource(MR.strings.anime_intro_times_go_to_episode),
                supportingText = episodes.firstOrNull { it.number == picked }?.let { episodeDetails(it) },
            )
        }
        // The service doesn't say which episodes there are, so their numbers are typed.
        else -> item(key = EPISODES_ITEM) {
            TypedEpisode(season = season, pick = loaded.pick, onPick = onPick)
        }
    }
    if (loaded.times != VideoPlayerMatchTimes.Unpicked) {
        item(key = TIMES_ITEM) {
            VideoPlayerMatchTimesCard(
                service = service,
                title = when {
                    choice.film -> choice.title
                    else -> loaded.pick?.let { episodeTitle(it, seasonTyped = choice.seasons.isEmpty()) }.orEmpty()
                },
                times = loaded.times,
                lengthMs = lengthMs,
                onRetry = onRetry,
                onUse = loaded.picked?.let { picked -> { onUse(picked) } },
            )
        }
    }
}

/**
 * Number fields for an episode the service's sources don't list: its number in [season], or both a season's number
 * and its own where no season is listed either. They start at [pick] and pick again whenever both hold a number.
 */
@Composable
private fun TypedEpisode(
    season: IntroTimesMatchSeason?,
    pick: IntroTimesMatchPick?,
    onPick: (IntroTimesMatchPick) -> Unit,
) {
    var typedSeason by rememberSaveable { mutableStateOf(pick?.season?.toString().orEmpty()) }
    var typedEpisode by rememberSaveable(season?.number) {
        mutableStateOf(pick?.takeIf { season == null || it.season == season.number }?.episode?.toString().orEmpty())
    }
    val typed = {
        val inSeason = season?.number ?: typedSeason.toIntOrNull()
        val episode = typedEpisode.toIntOrNull()?.takeIf { it >= 1 }
        if (inSeason != null && episode != null) onPick(IntroTimesMatchPick(inSeason, episode))
    }
    val numbers = KeyboardOptions(keyboardType = KeyboardType.Number)
    ControlRow(title = stringResource(MR.strings.anime_intro_times_which_episode)) {
        Row(horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.m)) {
            if (season == null) {
                TextField(
                    value = typedSeason,
                    onValueChange = {
                        typedSeason = it.filter(Char::isDigit)
                        typed()
                    },
                    modifier = Modifier.weight(1f),
                    label = stringResource(MR.strings.anime_intro_times_season),
                    keyboardOptions = numbers,
                )
            }
            TextField(
                value = typedEpisode,
                onValueChange = {
                    typedEpisode = it.filter(Char::isDigit)
                    typed()
                },
                modifier = Modifier.weight(1f),
                label = stringResource(MR.strings.anime_intro_times_episode),
                keyboardOptions = numbers,
            )
        }
    }
}

/** What the playing episode is called where it comes from: its season group, when it has one, and its own name. */
private val IntroTimesEpisode.playingName: String
    get() = listOfNotNull(scope.seasonName, name).joinToString(" · ")

@Composable
private fun seriesDetails(year: Int?, season: IntroTimesMatchSeason?): String? =
    listOfNotNull(
        year?.toString(),
        season?.episodes?.size?.let { pluralStringResource(MR.plurals.anime_num_episodes, it, it) },
    ).joinToString(" · ").ifEmpty { null }

@Composable
private fun episodeDetails(episode: IntroTimesMatchEpisode): String? {
    val dateFormat = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) }
    return listOfNotNull(episode.name, episode.aired?.format(dateFormat)).joinToString(" · ").ifEmpty { null }
}

@Composable
private fun episodeTitle(pick: IntroTimesMatchPick, seasonTyped: Boolean): String = when {
    seasonTyped -> stringResource(MR.strings.anime_intro_times_asked_season_episode, pick.season, pick.episode)
    else -> stringResource(MR.strings.anime_intro_times_episode_number, pick.episode)
}

private const val PLAYING_ITEM = "playing"
private const val STATUS_ITEM = "status"
private const val SEARCH_ITEM = "search"
private const val SERIES_ITEM = "series"
private const val SEASONS_ITEM = "seasons"
private const val EPISODES_ITEM = "episodes"
private const val TIMES_ITEM = "times"
