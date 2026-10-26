package eu.kanade.tachiyomi.ui.video.player.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
import coil3.compose.AsyncImage
import eu.kanade.tachiyomi.ui.video.player.introtimes.VideoPlayerSeriesSearchState
import mihon.entry.interactions.anime.introtimes.AniListSeries
import mihon.presentation.kit.button.Button
import mihon.presentation.kit.button.ButtonSize
import mihon.presentation.kit.button.ButtonStyle
import mihon.presentation.kit.content.CoverBacking
import mihon.presentation.kit.content.CoverFrame
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.content.coverFrame
import mihon.presentation.kit.content.rememberArtworkPlaceholder
import mihon.presentation.kit.feedback.LoadingScreen
import mihon.presentation.kit.feedback.NoResults
import mihon.presentation.kit.field.SearchField
import mihon.presentation.kit.list.CoverRow
import mihon.presentation.kit.list.RowGroupItem
import mihon.presentation.kit.list.RowGroupPosition
import mihon.presentation.kit.list.SettingsRow
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource

/**
 * The page of the settings where the series an episode's intro and credits times come from is picked. It searches
 * AniList, starting with the series' title; [currentAnilistId] is the series in use, and [pickedNone] marks None as the
 * one in use. Picking saves at once and returns to the settings.
 */
@Composable
internal fun VideoPlayerSeriesPage(
    frame: VideoPlayerSettingsFrame,
    initialQuery: String,
    currentAnilistId: Long?,
    pickedNone: Boolean,
    search: VideoPlayerSeriesSearchState,
    onSearch: (query: String, immediately: Boolean) -> Unit,
    onPick: (AniListSeries?) -> Unit,
    onBack: () -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by rememberSaveable { mutableStateOf(initialQuery) }
    LaunchedEffect(Unit) { onSearch(query, true) }
    BackHandler(onBack = onBack)
    val pick: (AniListSeries?) -> Unit = { series ->
        onPick(series)
        onBack()
    }
    Column(modifier = modifier) {
        VideoPlayerSettingsHeader(
            frame = frame,
            onDismissRequest = onDismissRequest,
            title = stringResource(MR.strings.anime_intro_times_series),
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
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = frame == VideoPlayerSettingsFrame.Panel),
            contentPadding = PaddingValues(bottom = KatariTheme.spacing.l),
        ) {
            when (search) {
                VideoPlayerSeriesSearchState.Searching -> item(key = STATUS_ITEM) {
                    LoadingScreen(Modifier.padding(vertical = KatariTheme.spacing.xl))
                }
                VideoPlayerSeriesSearchState.Failed -> item(key = STATUS_ITEM) {
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
                is VideoPlayerSeriesSearchState.Results -> if (search.series.isEmpty()) {
                    item(key = STATUS_ITEM) { NoResults() }
                } else {
                    itemsIndexed(search.series, key = { _, series -> series.anilistId }) { index, series ->
                        RowGroupItem(
                            position = RowGroupPosition.of(index, search.series.size),
                            selected = series.anilistId == currentAnilistId,
                        ) {
                            SeriesRow(series = series, onClick = { pick(series) })
                        }
                    }
                }
            }
            item(key = NONE_ITEM) {
                RowGroupItem(position = RowGroupPosition.of(0, 1), selected = pickedNone) {
                    CoverRow(
                        cover = { Box(Modifier.fillMaxSize().coverFrame(CoverFrame.Thumbnail, CoverBacking.Empty)) },
                        onClick = { pick(null) },
                    ) {
                        Text(stringResource(MR.strings.none), role = TextRole.ItemTitle)
                        Text(
                            stringResource(MR.strings.anime_intro_times_series_none_summary),
                            role = TextRole.Supporting,
                        )
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
