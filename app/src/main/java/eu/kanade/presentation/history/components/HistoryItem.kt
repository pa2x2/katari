package eu.kanade.presentation.history.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.entry.InlineEntryTypeIndicator
import eu.kanade.presentation.entry.components.EntryCover
import eu.kanade.presentation.entry.historySubtitle
import eu.kanade.presentation.history.HistoryUiItem
import eu.kanade.presentation.theme.TachiyomiPreviewTheme
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.DISABLED_ALPHA
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun HistoryListItem(
    item: HistoryUiItem,
    onClickCover: () -> Unit,
    onClickResume: () -> Unit,
    onClickDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val historyItem = item.historyItem

    Row(
        modifier = modifier
            .clickable(onClick = onClickResume)
            .height(56.dp)
            .padding(horizontal = MaterialTheme.padding.medium, vertical = MaterialTheme.padding.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        EntryCover.Square(
            modifier = Modifier.fillMaxHeight(),
            data = item.visibleCoverData,
            onClick = onClickCover,
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = MaterialTheme.padding.medium),
        ) {
            Text(
                text = item.visibleTitle,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                historyItem.entryType.InlineEntryTypeIndicator(
                    modifier = Modifier
                        .padding(end = 4.dp),
                )

                Text(
                    text = historyItem.entryType.historySubtitle(
                        childName = historyItem.history.chapterName,
                        childNumber = historyItem.history.chapterNumber,
                        consumedAt = historyItem.history.readAt,
                        consumedDuration = historyItem.history.readDuration,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalContentColor.current.copy(alpha = DISABLED_ALPHA),
                    modifier = Modifier.weight(weight = 1f, fill = false),
                )
            }
        }

        IconButton(onClick = onClickDelete) {
            Icon(
                imageVector = Icons.Outlined.Delete,
                contentDescription = stringResource(MR.strings.action_delete),
                tint = MaterialTheme.colorScheme.onSurface,
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
    TachiyomiPreviewTheme {
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
