package eu.kanade.tachiyomi.ui.video.player.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import app.katari.entry.interactions.anime.introtimes.aniskip.AniListSeries
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.rounded.Block
import app.katari.presentation.kit.button.Button
import app.katari.presentation.kit.button.ButtonSize
import app.katari.presentation.kit.button.ButtonStyle
import app.katari.presentation.kit.content.CoverBacking
import app.katari.presentation.kit.content.CoverFrame
import app.katari.presentation.kit.content.Text
import app.katari.presentation.kit.content.coverFrame
import app.katari.presentation.kit.content.rememberArtworkPlaceholder
import app.katari.presentation.kit.feedback.LoadingScreen
import app.katari.presentation.kit.feedback.NoResults
import app.katari.presentation.kit.field.SearchField
import app.katari.presentation.kit.layout.RowList
import app.katari.presentation.kit.list.CoverRow
import app.katari.presentation.kit.list.RowGroupGap
import app.katari.presentation.kit.list.RowGroupItem
import app.katari.presentation.kit.list.RowGroupPosition
import app.katari.presentation.kit.list.SettingsRow
import app.katari.presentation.kit.theme.KatariTheme
import app.katari.presentation.kit.theme.TextRole
import coil3.compose.AsyncImage
import eu.kanade.tachiyomi.ui.video.player.introtimes.VideoPlayerMatchSearchState
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource

/**
 * The page of the settings where the series an episode's AniSkip times come from is picked. It searches AniList,
 * starting with the series' title; [currentAnilistId] is the series in use, and [pickedNone] marks None as the
 * one in use. Picking saves at once and returns to the settings.
 */
@Composable
internal fun VideoPlayerAniSkipPage(
    frame: VideoPlayerSettingsFrame,
    initialQuery: String,
    currentAnilistId: Long?,
    pickedNone: Boolean,
    search: VideoPlayerMatchSearchState<AniListSeries>,
    onSearch: (query: String, immediately: Boolean) -> Unit,
    onPick: (AniListSeries?) -> Unit,
    onBack: () -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by rememberSaveable { mutableStateOf(initialQuery) }
    LaunchedEffect(Unit) { onSearch(query, true) }
    NavigationBackHandler(state = rememberNavigationEventState(NavigationEventInfo.None), onBackCompleted = onBack)
    val pick: (AniListSeries?) -> Unit = { series ->
        onPick(series)
        onBack()
    }
    Column(modifier = modifier) {
        VideoPlayerSettingsHeader(
            frame = frame,
            onDismissRequest = onDismissRequest,
            title = stringResource(
                MR.strings.anime_intro_times_series,
                stringResource(MR.strings.intro_times_service_aniskip),
            ),
            onBack = onBack,
        )
        SearchField(
            query = query,
            onQueryChange = {
                query = it
                onSearch(it, false)
            },
            placeholder = stringResource(MR.strings.anime_intro_times_search_placeholder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = KatariTheme.spacing.m, vertical = KatariTheme.spacing.s),
            onSearch = { onSearch(query, true) },
        )
        RowList(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = frame == VideoPlayerSettingsFrame.Panel),
            contentPadding = PaddingValues(bottom = KatariTheme.spacing.l),
        ) {
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
                    SettingsRow(
                        title = stringResource(MR.strings.anime_intro_times_search_failed),
                        trailing = {
                            Button(
                                label = stringResource(MR.strings.action_retry),
                                onClick = { onSearch(query, true) },
                                style = ButtonStyle.Text,
                                size = ButtonSize.Small,
                            )
                        },
                    )
                }
                is VideoPlayerMatchSearchState.Results -> if (search.found.isEmpty()) {
                    item(key = STATUS_ITEM) { NoResults() }
                } else {
                    itemsIndexed(search.found, key = { _, series -> series.anilistId }) { index, series ->
                        RowGroupItem(
                            position = RowGroupPosition.of(index, search.found.size),
                            selected = series.anilistId == currentAnilistId,
                        ) {
                            SeriesRow(series = series, onClick = { pick(series) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SeriesRow(series: AniListSeries, onClick: () -> Unit) {
    CoverRow(
        cover = {
            AsyncImage(
                model = series.coverUrl,
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
        Text(series.title, role = TextRole.ItemTitle, maxLines = 2, overflow = TextOverflow.Ellipsis)
        val details = listOfNotNull(
            series.format?.let { formatLabel(it) },
            series.year?.toString(),
            series.episodes?.let { pluralStringResource(MR.plurals.anime_num_episodes, it, it) },
        )
        if (details.isNotEmpty()) {
            Text(
                details.joinToString(" · "),
                role = TextRole.Supporting,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// AniList's MediaFormat values for anime.
@Composable
private fun formatLabel(format: String): String? = when (format) {
    "TV" -> stringResource(MR.strings.anime_series_format_tv)
    "TV_SHORT" -> stringResource(MR.strings.anime_series_format_tv_short)
    "MOVIE" -> stringResource(MR.strings.anime_series_format_movie)
    "SPECIAL" -> stringResource(MR.strings.anime_series_format_special)
    "OVA" -> stringResource(MR.strings.anime_series_format_ova)
    "ONA" -> stringResource(MR.strings.anime_series_format_ona)
    "MUSIC" -> stringResource(MR.strings.anime_series_format_music)
    else -> null
}

private const val STATUS_ITEM = "status"
private const val NONE_ITEM = "none"
private const val NONE_GAP_ITEM = "none-gap"
