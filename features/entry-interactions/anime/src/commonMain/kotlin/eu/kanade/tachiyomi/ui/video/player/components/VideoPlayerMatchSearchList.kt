package eu.kanade.tachiyomi.ui.video.player.components

import androidx.compose.foundation.layout.ColumnScope
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
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.rounded.Block
import app.katari.presentation.kit.feedback.LoadingScreen
import app.katari.presentation.kit.feedback.NoResults
import app.katari.presentation.kit.field.SearchField
import app.katari.presentation.kit.list.RowGroupGap
import app.katari.presentation.kit.list.RowGroupItem
import app.katari.presentation.kit.list.RowGroupPosition
import app.katari.presentation.kit.list.SettingsRow
import app.katari.presentation.kit.theme.KatariTheme
import eu.kanade.tachiyomi.ui.video.player.introtimes.VideoPlayerMatchSearchState
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * The search of a match page: it starts with [initialQuery], the series' name, and lists None first and then what the
 * service's search finds. [pickedNone] marks None as the one in use, and [selected] the candidate that is.
 */
@Composable
internal fun <C, M> ColumnScope.VideoPlayerMatchSearchList(
    frame: VideoPlayerSettingsFrame,
    initialQuery: String,
    search: VideoPlayerMatchSearchState<C>,
    onSearch: (query: String, immediately: Boolean) -> Unit,
    pickedNone: Boolean,
    selected: (C) -> Boolean,
    content: VideoPlayerMatchPageContent<C, M>,
    onPickNone: () -> Unit,
    onPick: (C) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf(initialQuery) }
    LaunchedEffect(Unit) { onSearch(query, true) }
    SearchField(
        query = query,
        onQueryChange = {
            query = it
            onSearch(it, false)
        },
        placeholder = content.searchPlaceholder,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = KatariTheme.spacing.m, vertical = KatariTheme.spacing.s),
        onSearch = { onSearch(query, true) },
    )
    VideoPlayerMatchList(frame) {
        // First, so it's in reach whatever the search finds.
        item(key = NONE_ITEM) {
            RowGroupItem(position = RowGroupPosition.of(0, 1), selected = pickedNone) {
                SettingsRow(
                    title = stringResource(MR.strings.none),
                    subtitle = stringResource(MR.strings.anime_intro_times_series_none_summary),
                    icon = MaterialSymbols.Rounded.Block,
                    onClick = onPickNone,
                )
            }
        }
        item(key = NONE_GAP_ITEM) { RowGroupGap() }
        when (search) {
            VideoPlayerMatchSearchState.Searching -> item(key = STATUS_ITEM) {
                LoadingScreen(Modifier.padding(vertical = KatariTheme.spacing.xl))
            }
            VideoPlayerMatchSearchState.Failed -> item(key = STATUS_ITEM) {
                VideoPlayerMatchFailedRow(content.searchFailed) { onSearch(query, true) }
            }
            is VideoPlayerMatchSearchState.Results -> if (search.found.isEmpty()) {
                item(key = STATUS_ITEM) { NoResults() }
            } else {
                itemsIndexed(search.found, key = { index, _ -> "candidate_$index" }) { index, candidate ->
                    RowGroupItem(
                        position = RowGroupPosition.of(index, search.found.size),
                        selected = selected(candidate),
                    ) {
                        content.candidateRow(candidate) { onPick(candidate) }
                    }
                }
            }
        }
        content.credit(this)
    }
}

private const val STATUS_ITEM = "status"
private const val NONE_ITEM = "none"
private const val NONE_GAP_ITEM = "none-gap"
