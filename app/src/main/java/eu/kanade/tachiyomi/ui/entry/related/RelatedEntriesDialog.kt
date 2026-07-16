package eu.kanade.tachiyomi.ui.entry.related

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import eu.kanade.presentation.browse.components.CatalogBadges
import eu.kanade.presentation.entry.components.toGridCoverType
import eu.kanade.presentation.library.components.CommonEntryItemDefaults
import eu.kanade.presentation.library.components.EntryComfortableGridItem
import eu.kanade.presentation.util.formattedMessage
import eu.kanade.tachiyomi.source.entry.EntryItemOrientation
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryCover
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.EmptyScreen
import tachiyomi.presentation.core.screens.EmptyScreenAction
import tachiyomi.presentation.core.screens.LoadingScreen

@Composable
fun RelatedEntriesDialog(
    screenModel: RelatedEntriesScreenModel,
    sourceItemOrientation: EntryItemOrientation,
    onDismissRequest: () -> Unit,
    onEntryClick: (Entry) -> Unit,
) {
    val context = LocalContext.current
    val state by screenModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(screenModel) {
        screenModel.load()
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.82f)
                .widthIn(max = 840.dp),
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp,
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(MR.strings.related_entries),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = onDismissRequest) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = stringResource(MR.strings.action_close),
                        )
                    }
                }
                HorizontalDivider()

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                ) {
                    when (val currentState = state) {
                        RelatedEntriesScreenModel.State.Idle,
                        RelatedEntriesScreenModel.State.Loading,
                        -> LoadingScreen(Modifier.fillMaxSize())

                        is RelatedEntriesScreenModel.State.Error -> EmptyScreen(
                            message = with(context) { currentState.throwable.formattedMessage },
                            modifier = Modifier.fillMaxSize(),
                            actions = listOf(
                                EmptyScreenAction(
                                    stringRes = MR.strings.action_retry,
                                    icon = Icons.Outlined.Refresh,
                                    onClick = screenModel::retry,
                                ),
                            ),
                        )

                        is RelatedEntriesScreenModel.State.Success -> {
                            if (currentState.relatedEntries.isEmpty()) {
                                EmptyScreen(
                                    message = stringResource(MR.strings.no_results_found),
                                    modifier = Modifier.fillMaxSize(),
                                    actions = listOf(
                                        EmptyScreenAction(
                                            stringRes = MR.strings.action_retry,
                                            icon = Icons.Outlined.Refresh,
                                            onClick = screenModel::retry,
                                        ),
                                    ),
                                )
                            } else {
                                RelatedEntriesContent(
                                    entries = currentState.relatedEntries,
                                    sourceItemOrientation = sourceItemOrientation,
                                    entryState = screenModel::getEntryState,
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
    val minimumItemWidth = if (sourceItemOrientation == EntryItemOrientation.HORIZONTAL) 180.dp else 128.dp
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minimumItemWidth),
        contentPadding = PaddingValues(8.dp),
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
                onLongClick = { onEntryClick(entry) },
                onClick = { onEntryClick(entry) },
            )
        }
    }
}
