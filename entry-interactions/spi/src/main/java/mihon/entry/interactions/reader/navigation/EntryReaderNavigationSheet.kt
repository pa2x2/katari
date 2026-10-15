package mihon.entry.interactions.reader.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import mihon.entry.interactions.child.EntryChildProgressLabel
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.roundedfilled.PlayArrow
import mihon.presentation.kit.content.Icon
import mihon.presentation.kit.list.ChapterRow
import mihon.presentation.kit.sheet.Sheet
import mihon.presentation.kit.sheet.SheetHeader
import mihon.presentation.kit.theme.ContentTone
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

data class EntryReaderNavigationRow<T>(
    val item: T,
    val title: String,
    val read: Boolean? = null,
    val bookmark: Boolean = false,
    val progressLabel: EntryChildProgressLabel? = null,
    val depth: Int = 0,
)

/** Shared table-of-contents surface for built-in readers. */
@Composable
fun <T> EntryReaderNavigationSheet(
    visible: Boolean,
    rows: List<EntryReaderNavigationRow<T>>,
    selectedIndex: Int,
    onItemClick: (T) -> Unit,
    onDismissRequest: () -> Unit,
) {
    if (!visible) return

    val listState = rememberLazyListState()
    LaunchedEffect(visible, rows, selectedIndex) {
        if (visible && selectedIndex >= 0) listState.scrollToItem(selectedIndex)
    }

    Sheet(
        onDismissRequest = onDismissRequest,
        capHeight = true,
    ) {
        Column {
            SheetHeader(title = stringResource(MR.strings.reader_table_of_contents))
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
                state = listState,
            ) {
                itemsIndexed(rows) { index, row ->
                    val selected = index == selectedIndex
                    ChapterRow(
                        title = row.title,
                        read = row.read,
                        bookmark = row.bookmark,
                        onClick = {
                            onItemClick(row.item)
                            onDismissRequest()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        progress = row.progressLabel?.let { label ->
                            stringResource(label.resource, *label.args.toTypedArray())
                        },
                        selected = selected,
                        depth = row.depth,
                        trailing = if (selected) {
                            {
                                Icon(
                                    imageVector = MaterialSymbols.RoundedFilled.PlayArrow,
                                    contentDescription = null,
                                    tone = ContentTone.Primary,
                                )
                            }
                        } else {
                            null
                        },
                    )
                }
            }
        }
    }
}
