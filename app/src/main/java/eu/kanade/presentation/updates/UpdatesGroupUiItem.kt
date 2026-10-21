package eu.kanade.presentation.updates

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import eu.kanade.presentation.entry.InlineEntryTypeIndicator
import eu.kanade.presentation.entry.components.ChapterDownloadAction
import eu.kanade.presentation.entry.components.EntryChapterDownloadIndicator
import eu.kanade.presentation.entry.entryTypePresentation
import eu.kanade.tachiyomi.ui.updates.UpdatesItem
import mihon.entry.interactions.download.EntryDownloadState
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.KeyboardArrowDown
import mihon.icons.materialsymbols.rounded.KeyboardArrowUp
import mihon.presentation.kit.content.InlineIcon
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.list.ChapterMarks
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource

/**
 * One row for an entry's updates of one day. Tapping it shows or hides them, which the chevron after the count tells;
 * selecting it, or downloading from it, acts on all of them.
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
        // A merged entry's group can span members; name one only when all the updates belong to it.
        memberTitle = items.map { it.memberTitle }.distinct().singleOrNull(),
        coverData = first.visibleCoverData,
        selected = selected,
        read = items.all { it.update.consumed },
        onClick = {
            if (selectionMode) onGroupSelected(items, !selected) else onToggleExpanded()
        },
        onLongClick = { onGroupSelected(items, !selected) },
        modifier = modifier,
        onClickCover = { onClickCover(first) }.takeIf { !selectionMode },
        subtitle = { tone ->
            entryType.InlineEntryTypeIndicator()
            ChapterMarks(
                unread = items.any { !it.update.consumed },
                bookmark = false,
                unreadLabel = stringResource(entryType.entryTypePresentation().unconsumedIndicatorLabel),
                role = TextRole.Supporting,
            )
            Text(
                text = pluralStringResource(
                    entryType.entryTypePresentation().childCountPlural,
                    items.size,
                    items.size.toString(),
                ),
                role = TextRole.Supporting,
                tone = tone,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            InlineIcon(
                imageVector = if (group.expanded) {
                    MaterialSymbols.Rounded.KeyboardArrowUp
                } else {
                    MaterialSymbols.Rounded.KeyboardArrowDown
                },
                contentDescription = stringResource(
                    if (group.expanded) MR.strings.action_collapse else MR.strings.action_expand,
                ),
                role = TextRole.Supporting,
                tone = tone,
            )
        },
        trailing = if (!selectionMode && items.all { it.downloadAvailable }) {
            {
                EntryChapterDownloadIndicator(
                    enabled = true,
                    modifier = Modifier.padding(start = KatariTheme.spacing.xs),
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
        } else {
            null
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
