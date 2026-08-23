package eu.kanade.presentation.library.statistics

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.entry.components.toListCoverType
import eu.kanade.presentation.library.components.EntryListItem
import eu.kanade.presentation.library.components.EntryTypeBadge
import eu.kanade.tachiyomi.source.entry.EntryItemOrientation
import eu.kanade.tachiyomi.ui.library.statistics.LibraryStatisticsScreenModel
import tachiyomi.domain.entry.model.asEntryCover
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.FastScrollLazyColumn
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.LoadingScreen

@Composable
fun LibraryStatisticsScreenContent(
    state: LibraryStatisticsScreenModel.State,
    paddingValues: PaddingValues,
    showType: Boolean,
    onEntryClick: (Long) -> Unit,
) {
    when (state) {
        LibraryStatisticsScreenModel.State.Loading -> LoadingScreen(Modifier.padding(paddingValues))
        LibraryStatisticsScreenModel.State.Failed -> CenteredMessage(
            paddingValues = paddingValues,
            message = stringResource(MR.strings.statistics_could_not_load_titles),
        )
        is LibraryStatisticsScreenModel.State.Success -> {
            if (state.items.isEmpty()) {
                CenteredMessage(
                    paddingValues = paddingValues,
                    message = stringResource(MR.strings.information_no_entries_found),
                )
                return
            }
            FastScrollLazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = paddingValues.calculateTopPadding() + 8.dp,
                    bottom = paddingValues.calculateBottomPadding() + 8.dp,
                ),
            ) {
                items(
                    items = state.items,
                    key = { it.key.toString() },
                    contentType = { "statistics_library_item" },
                ) { item ->
                    val fitCover = item.sourceItemOrientation == EntryItemOrientation.HORIZONTAL
                    EntryListItem(
                        title = item.title,
                        coverData = item.entry.asEntryCover(),
                        coverType = item.sourceItemOrientation.toListCoverType(),
                        coverContentScale = if (fitCover) ContentScale.Fit else ContentScale.Crop,
                        coverBackgroundColor = if (fitCover) {
                            MaterialTheme.colorScheme.surfaceContainerHigh
                        } else {
                            Color.Transparent
                        },
                        badge = {
                            if (showType) EntryTypeBadge(item.entry.type)
                        },
                        onClick = { onEntryClick(item.entry.id) },
                        onLongClick = { onEntryClick(item.entry.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun CenteredMessage(paddingValues: PaddingValues, message: String) {
    Box(
        modifier = Modifier.fillMaxSize().padding(paddingValues),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.material3.Text(
            text = message,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
