package mihon.entry.interactions.book

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.AdaptiveSheet
import tachiyomi.presentation.core.i18n.stringResource

internal data class BookReaderNavigationRow<T>(
    val item: T,
    val title: String,
    val depth: Int = 0,
)

/** Shared table-of-contents surface for built-in BOOK readers. */
@Composable
internal fun <T> BookReaderNavigationSheet(
    visible: Boolean,
    rows: List<BookReaderNavigationRow<T>>,
    selectedIndex: Int,
    onItemClick: (T) -> Unit,
    onDismissRequest: () -> Unit,
) {
    if (!visible) return

    val listState = rememberLazyListState()
    LaunchedEffect(visible, selectedIndex) {
        if (visible && selectedIndex >= 0) listState.scrollToItem(selectedIndex)
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = true),
    ) {
        BoxWithConstraints {
            AdaptiveSheet(
                isTabletUi = false,
                enableImplicitDismiss = true,
                onDismissRequest = onDismissRequest,
                modifier = Modifier.heightIn(max = maxHeight * 0.85f),
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 24.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ViewList,
                            contentDescription = null,
                        )
                        Text(
                            text = stringResource(MR.strings.book_table_of_contents),
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 16.dp),
                            style = MaterialTheme.typography.titleLarge,
                        )
                        IconButton(onClick = onDismissRequest) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = stringResource(MR.strings.action_close),
                            )
                        }
                    }
                    HorizontalDivider()
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false),
                        state = listState,
                    ) {
                        itemsIndexed(rows) { index, row ->
                            val selected = index == selectedIndex
                            Text(
                                text = row.title,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        if (selected) {
                                            MaterialTheme.colorScheme.secondaryContainer
                                        } else {
                                            Color.Transparent
                                        },
                                    )
                                    .clickable {
                                        onItemClick(row.item)
                                        onDismissRequest()
                                    }
                                    .padding(
                                        start = 16.dp + (row.depth * 16).dp,
                                        top = 12.dp,
                                        end = 16.dp,
                                        bottom = 12.dp,
                                    ),
                                color = if (selected) {
                                    MaterialTheme.colorScheme.onSecondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }
        }
    }
}
