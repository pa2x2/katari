package eu.kanade.tachiyomi.ui.entry.related

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import eu.kanade.presentation.browse.components.CatalogBadges
import eu.kanade.presentation.entry.components.toGridCoverType
import eu.kanade.presentation.library.components.CommonEntryItemDefaults
import eu.kanade.presentation.library.components.EntryComfortableGridItem
import eu.kanade.presentation.util.formattedMessage
import eu.kanade.tachiyomi.source.entry.EntryItemOrientation
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Refresh
import mihon.presentation.kit.content.BrowseMetrics
import mihon.presentation.kit.feedback.EmptyScreen
import mihon.presentation.kit.feedback.EmptyScreenAction
import mihon.presentation.kit.feedback.LoadingScreen
import mihon.presentation.kit.progress.PullRefresh
import mihon.presentation.kit.sheet.Sheet
import mihon.presentation.kit.sheet.SheetHeader
import mihon.presentation.kit.theme.KatariTheme
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryCover
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun RelatedEntriesDialog(
    viewModel: RelatedEntriesViewModel,
    onDismissRequest: () -> Unit,
    onEntryClick: (Entry) -> Unit,
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.load()
    }

    // A sheet capped below the window's height; the grid fills it, so loading and empty states keep its size.
    Sheet(onDismissRequest = onDismissRequest, capHeight = true) {
        Column(modifier = Modifier.fillMaxHeight()) {
            SheetHeader(title = stringResource(MR.strings.related_entries))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                when (val currentState = state) {
                    RelatedEntriesViewModel.State.Idle,
                    RelatedEntriesViewModel.State.Loading,
                    -> LoadingScreen(Modifier.fillMaxSize())

                    is RelatedEntriesViewModel.State.Error -> EmptyScreen(
                        message = with(context) { currentState.throwable.formattedMessage },
                        modifier = Modifier.fillMaxSize(),
                        actions = listOf(
                            EmptyScreenAction(
                                stringRes = MR.strings.action_retry,
                                icon = MaterialSymbols.Rounded.Refresh,
                                onClick = viewModel::retry,
                            ),
                        ),
                    )

                    is RelatedEntriesViewModel.State.Unavailable -> EmptyScreen(
                        message = stringResource(MR.strings.no_results_found),
                        modifier = Modifier.fillMaxSize(),
                        actions = listOf(
                            EmptyScreenAction(
                                stringRes = MR.strings.action_retry,
                                icon = MaterialSymbols.Rounded.Refresh,
                                onClick = viewModel::retry,
                            ),
                        ),
                    )

                    is RelatedEntriesViewModel.State.Success -> {
                        if (currentState.relatedEntries.isEmpty()) {
                            EmptyScreen(
                                message = stringResource(MR.strings.no_results_found),
                                modifier = Modifier.fillMaxSize(),
                                actions = listOf(
                                    EmptyScreenAction(
                                        stringRes = MR.strings.action_retry,
                                        icon = MaterialSymbols.Rounded.Refresh,
                                        onClick = viewModel::retry,
                                    ),
                                ),
                            )
                        } else {
                            PullRefresh(
                                refreshing = currentState.isRefreshing,
                                enabled = !currentState.isRefreshing,
                                onRefresh = viewModel::refresh,
                                modifier = Modifier.fillMaxSize(),
                            ) {
                                RelatedEntriesContent(
                                    entries = currentState.relatedEntries,
                                    sourceItemOrientation = currentState.sourceItemOrientation,
                                    entryState = viewModel::getEntryState,
                                    onEntryClick = onEntryClick,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RelatedEntriesContent(
    entries: List<Entry>,
    sourceItemOrientation: EntryItemOrientation,
    entryState: @Composable (Entry) -> androidx.compose.runtime.State<Entry>,
    onEntryClick: (Entry) -> Unit,
) {
    val minimumItemWidth = if (sourceItemOrientation == EntryItemOrientation.HORIZONTAL) {
        BrowseMetrics.wideCoverGridMinCellWidth
    } else {
        BrowseMetrics.coverGridMinCellWidth
    }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minimumItemWidth),
        contentPadding = PaddingValues(KatariTheme.spacing.s),
        verticalArrangement = Arrangement.spacedBy(CommonEntryItemDefaults.GridVerticalSpacer),
        horizontalArrangement = Arrangement.spacedBy(CommonEntryItemDefaults.GridHorizontalSpacer),
    ) {
        items(
            items = entries,
            key = { "${it.type}:${it.id}" },
        ) { initialEntry ->
            val entry by entryState(initialEntry)
            EntryComfortableGridItem(
                title = entry.displayTitle,
                coverData = EntryCover(
                    entryId = entry.id,
                    sourceId = entry.source,
                    isFavorite = entry.favorite,
                    url = entry.thumbnailUrl,
                    lastModified = entry.coverLastModified,
                ),
                coverType = sourceItemOrientation.toGridCoverType(),
                coverAlpha = if (entry.favorite) CommonEntryItemDefaults.BrowseFavoriteCoverAlpha else 1f,
                coverBadgeEnd = {
                    CatalogBadges(isFavorite = entry.favorite, entryType = entry.type)
                },
                onLongClick = null,
                onClick = { onEntryClick(entry) },
            )
        }
    }
}
