package eu.kanade.presentation.history.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import mihon.presentation.kit.content.LibraryMetrics
import mihon.presentation.kit.content.Text
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

    Row(
        modifier = modifier
            .then(
                if (onClickResume != null) {
                    Modifier.clickable(onClick = onClickResume)
                } else {
                    Modifier
                },
            )
            .height(if (item.memberTitle != null) LibraryMetrics.listRowTallHeight else LibraryMetrics.listRowHeight)
            .padding(horizontal = KatariTheme.spacing.contentInset, vertical = KatariTheme.spacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        EntryCover.Square(
            modifier = Modifier.fillMaxHeight(),
            data = item.visibleCoverData,
            shape = KatariTheme.shapes.coverThumbnail,
            onClick = onClickCover,
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = KatariTheme.spacing.l),
        ) {
            Text(
                text = item.visibleTitle,
                role = TextRole.Body,
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

        IconButton(
            icon = MaterialSymbols.Rounded.Delete,
            contentDescription = stringResource(MR.strings.action_delete),
            onClick = onClickDelete,
        )
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
