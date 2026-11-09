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
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.rounded.Delete
import app.katari.presentation.kit.button.IconButton
import app.katari.presentation.kit.content.CoverFrame
import app.katari.presentation.kit.content.Text
import app.katari.presentation.kit.list.CoverRow
import app.katari.presentation.kit.selection.ItemAction
import app.katari.presentation.kit.selection.selectableItem
import app.katari.presentation.kit.surface.Surface
import app.katari.presentation.kit.theme.ContentTone
import app.katari.presentation.kit.theme.KatariPreviewTheme
import app.katari.presentation.kit.theme.KatariTheme
import app.katari.presentation.kit.theme.TextRole
import eu.kanade.presentation.entry.InlineEntryTypeIndicator
import eu.kanade.presentation.entry.components.EntryCover
import eu.kanade.presentation.entry.historySubtitle
import eu.kanade.presentation.history.HistoryUiItem
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun HistoryListItem(
    item: HistoryUiItem,
    /** The other device the item was last read on; null when it was this one. */
    readOn: String?,
    timeFormatter: DateTimeFormatter,
    onClickCover: () -> Unit,
    onClickResume: (() -> Unit)?,
    onClickDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val historyItem = item.historyItem
    val delete = ItemAction(
        label = stringResource(MR.strings.action_delete),
        icon = MaterialSymbols.Rounded.Delete,
        onClick = onClickDelete,
    )

    CoverRow(
        cover = {
            EntryCover.Book(
                modifier = Modifier.fillMaxSize(),
                data = item.visibleCoverData,
                frame = CoverFrame.Thumbnail,
                onClick = onClickCover,
            )
        },
        modifier = modifier.selectableItem(
            selected = false,
            onToggle = null,
            onSelectRange = null,
            onOpen = onClickResume,
            actions = listOf(delete),
        ),
        onClick = onClickResume,
        trailing = {
            IconButton(
                icon = delete.icon,
                contentDescription = delete.label,
                onClick = delete.onClick,
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
                    childGroupTitle = historyItem.history.chapterGroupTitle,
                    timeFormatter = timeFormatter,
                ),
                modifier = Modifier.weight(weight = 1f, fill = false),
                role = TextRole.Supporting,
                tone = ContentTone.Disabled,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            readOn?.let { device ->
                Text(
                    text = stringResource(MR.strings.history_read_on_device, device),
                    role = TextRole.Supporting,
                    tone = ContentTone.Disabled,
                    maxLines = 1,
                )
            }
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
                readOn = null,
                timeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT),
                onClickCover = {},
                onClickResume = {},
                onClickDelete = {},
            )
        }
    }
}
