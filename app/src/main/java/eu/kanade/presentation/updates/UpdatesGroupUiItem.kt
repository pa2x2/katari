package eu.kanade.presentation.updates

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.entry.InlineEntryTypeIndicator
import eu.kanade.presentation.entry.components.ChapterDownloadAction
import eu.kanade.presentation.entry.components.EntryChapterDownloadIndicator
import eu.kanade.presentation.entry.entryTypePresentation
import eu.kanade.tachiyomi.ui.updates.UpdatesItem
import mihon.entry.interactions.download.EntryDownloadState
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource

/**
 * One row for an entry's updates of one day. Tapping it shows or hides them; selecting it, or downloading from it,
 * acts on all of them.
 */
@Composable
internal fun UpdatesGroupUiItem(
    group: UpdatesUiModel.Group<UpdatesItem>,
    selectionMode: Boolean,
    onToggleExpanded: () -> Unit,
    onGroupSelected: (List<UpdatesItem>, Boolean) -> Unit,
    onClickCover: (UpdatesItem) -> Unit,
    onDownloadChapter: (List<UpdatesItem>, ChapterDownloadAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val items = group.items
    val first = items.first()
    val entryType = first.update.entryType
    val selected = items.all { it.selected }
    UpdatesBaseUiItem(
        title = first.visibleEntryTitle,
        coverData = first.visibleCoverData,
        selected = selected,
        read = items.all { it.update.consumed },
        onClick = {
            if (selectionMode) onGroupSelected(items, !selected) else onToggleExpanded()
        },
        onLongClick = { onGroupSelected(items, !selected) },
        modifier = modifier,
        onClickCover = { onClickCover(first) }.takeIf { !selectionMode },
        subtitle = { textAlpha ->
            entryType.InlineEntryTypeIndicator(modifier = Modifier.padding(end = 4.dp))
            if (items.any { !it.update.consumed }) {
                Icon(
                    imageVector = Icons.Filled.Circle,
                    contentDescription = stringResource(entryType.entryTypePresentation().unconsumedIndicatorLabel),
                    modifier = Modifier
                        .height(8.dp)
                        .padding(end = 4.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            Text(
                text = pluralStringResource(
                    entryType.entryTypePresentation().childCountPlural,
                    items.size,
                    items.size.toString(),
                ),
                maxLines = 1,
                style = MaterialTheme.typography.bodySmall,
                color = LocalContentColor.current.copy(alpha = textAlpha),
                overflow = TextOverflow.Ellipsis,
            )
        },
        trailing = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!selectionMode && items.all { it.downloadAvailable }) {
                    EntryChapterDownloadIndicator(
                        enabled = true,
                        modifier = Modifier.padding(start = 4.dp),
                        downloadStateProvider = { items.groupDownloadState() },
                        downloadProgressProvider = {
                            items.firstOrNull { it.downloadStateProvider() == EntryDownloadState.DOWNLOADING }
                                ?.downloadProgressProvider
                                ?.invoke()
                                ?: 0
                        },
                        onClick = { action ->
                            val targets = when (action) {
                                ChapterDownloadAction.START -> items.filter {
                                    it.downloadStateProvider() != EntryDownloadState.DOWNLOADED
                                }
                                else -> items
                            }
                            onDownloadChapter(targets, action)
                        },
                    )
                }
                IconButton(onClick = onToggleExpanded) {
                    Icon(
                        imageVector = if (group.expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                        contentDescription = stringResource(
                            if (group.expanded) MR.strings.action_collapse else MR.strings.action_expand,
                        ),
                    )
                }
            }
        },
    )
}

/** Downloaded once all are; otherwise in progress while any is, so the group's downloads can be followed or cancelled. */
private fun List<UpdatesItem>.groupDownloadState(): EntryDownloadState {
    val states = map { it.downloadStateProvider() }
    return when {
        states.all { it == EntryDownloadState.DOWNLOADED } -> EntryDownloadState.DOWNLOADED
        EntryDownloadState.DOWNLOADING in states -> EntryDownloadState.DOWNLOADING
        EntryDownloadState.QUEUE in states -> EntryDownloadState.QUEUE
        EntryDownloadState.ERROR in states -> EntryDownloadState.ERROR
        else -> EntryDownloadState.NOT_DOWNLOADED
    }
}
