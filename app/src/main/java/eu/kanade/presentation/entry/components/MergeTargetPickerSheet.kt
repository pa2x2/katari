package eu.kanade.presentation.entry.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.collections.immutable.ImmutableList
import mihon.presentation.kit.content.CoverFrame
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.feedback.EmptyScreen
import mihon.presentation.kit.field.SearchField
import mihon.presentation.kit.list.CoverRow
import mihon.presentation.kit.list.RowGroupItem
import mihon.presentation.kit.list.RowGroupPosition
import mihon.presentation.kit.sheet.Sheet
import mihon.presentation.kit.sheet.SheetHeader
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** Picks the library entry to merge into: a search over the candidates, nearest matches first. */
@Composable
fun MergeTargetPickerSheet(
    title: String,
    query: String,
    entries: ImmutableList<MergeEditorEntry>,
    onDismissRequest: () -> Unit,
    onQueryChange: (String) -> Unit,
    onSelectTarget: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Sheet(onDismissRequest = onDismissRequest, modifier = modifier, capHeight = true) {
        Column(modifier = Modifier.fillMaxWidth()) {
            SheetHeader(title = title)
            SearchField(
                query = query,
                onQueryChange = onQueryChange,
                placeholder = stringResource(MR.strings.action_search),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = KatariTheme.spacing.m, vertical = KatariTheme.spacing.s),
            )
            if (entries.isEmpty()) {
                EmptyScreen(
                    message = stringResource(MR.strings.merge_target_picker_empty, query),
                    modifier = Modifier.weight(1f, fill = false),
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    contentPadding = PaddingValues(vertical = KatariTheme.spacing.m),
                ) {
                    itemsIndexed(entries, key = { _, entry -> entry.id }) { index, entry ->
                        RowGroupItem(position = RowGroupPosition.of(index, entries.size)) {
                            CoverRow(
                                cover = {
                                    EntryCover.Book(
                                        modifier = Modifier.fillMaxSize(),
                                        data = entry.coverData,
                                        frame = CoverFrame.Thumbnail,
                                    )
                                },
                                onClick = { onSelectTarget(entry.id) },
                            ) {
                                Text(
                                    text = entry.title,
                                    role = TextRole.ItemTitle,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = mergeEditorSubtitle(entry),
                                    role = TextRole.Supporting,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
