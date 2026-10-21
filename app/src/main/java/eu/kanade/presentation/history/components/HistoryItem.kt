package eu.kanade.presentation.history.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import eu.kanade.presentation.entry.InlineEntryTypeIndicator
import eu.kanade.presentation.entry.components.EntryCover
import eu.kanade.presentation.entry.historySubtitle
import eu.kanade.presentation.history.HistoryUiItem
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Delete
import mihon.presentation.kit.button.IconButton
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.list.CoverRow
import mihon.presentation.kit.surface.Surface
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariPreviewTheme
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun HistoryListItem(
    item: HistoryUiItem,
    onClickCover: () -> Unit,
    onClickResume: (() -> Unit)?,
    onClickDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val historyItem = item.historyItem

    CoverRow(
        cover = {
            EntryCover.Book(
                modifier = Modifier.fillMaxSize(),
                data = item.visibleCoverData,
                shape = KatariTheme.shapes.coverThumbnail,
                onClick = onClickCover,
            )
        },
        modifier = modifier,
        onClick = onClickResume,
        trailing = {
            IconButton(
                icon = MaterialSymbols.Rounded.Delete,
                contentDescription = stringResource(MR.strings.action_delete),
                onClick = onClickDelete,
                tone = ContentTone.Secondary,
            )
        },
    ) {
        Text(
            text = item.visibleTitle,
            role = TextRole.ItemTitle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        item.memberTitle?.let { memberTitle ->
            Text(
                text = memberTitle,
                role = TextRole.Supporting,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            historyItem.entryType.InlineEntryTypeIndicator()

            Text(
                text = historyItem.entryType.historySubtitle(
                    childName = historyItem.history.chapterName,
                    childNumber = historyItem.history.chapterNumber,
                    consumedAt = historyItem.history.readAt,
                    consumedDuration = historyItem.history.readDuration,
                ),
                modifier = Modifier.weight(weight = 1f, fill = false),
                role = TextRole.Supporting,
                tone = ContentTone.Disabled,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun HistoryListItemPreviews(
    @PreviewParameter(HistoryItemProvider::class)
    item: HistoryUiItem,
) {
    KatariPreviewTheme {
        Surface {
            HistoryListItem(
                item = item,
                onClickCover = {},
                onClickResume = {},
                onClickDelete = {},
            )
        }
    }
}
