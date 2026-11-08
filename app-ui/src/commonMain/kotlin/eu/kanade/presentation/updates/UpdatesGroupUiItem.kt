package eu.kanade.presentation.updates

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import app.katari.entry.interactions.download.EntryDownloadState
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.rounded.KeyboardArrowDown
import app.katari.icons.materialsymbols.rounded.KeyboardArrowUp
import app.katari.presentation.kit.content.InlineIcon
import app.katari.presentation.kit.content.Text
import app.katari.presentation.kit.list.ChapterMarks
import app.katari.presentation.kit.selection.selectableItem
import app.katari.presentation.kit.theme.KatariTheme
import app.katari.presentation.kit.theme.TextRole
import eu.kanade.presentation.entry.InlineEntryTypeIndicator
import eu.kanade.presentation.entry.components.ChapterDownloadAction
import eu.kanade.presentation.entry.components.EntryChapterDownloadIndicator
import eu.kanade.presentation.entry.entryTypePresentation
import eu.kanade.tachiyomi.ui.updates.UpdatesItem
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
        // The row selects all its updates at once, so it has no range of its own for Shift+click
        modifier = modifier.selectableItem(
            selected = selected,
            onToggle = { onGroupSelected(items, !selected) },
            onSelectRange = null,
            onOpen = null,
        ),
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
                    downloadProgressProvider = { items.groupDownloadProgress() },
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

/**
 * How far the group's downloads have come, 0–100: downloaded updates count in full and the downloading ones by their
 * own progress, out of every update that is downloaded or waiting to be. Updates left out of the download, or failed,
 * don't count, so the ring fills once over the whole batch and ends full when the queue is done. It is kept while the
 * next chapter waits in the queue, as it does while the downloader fetches that chapter's pages.
 */
private fun List<UpdatesItem>.groupDownloadProgress(): Int {
    var total = 0
    var done = 0
    for (item in this) {
        when (item.downloadStateProvider()) {
            EntryDownloadState.DOWNLOADED -> done += 100
            EntryDownloadState.DOWNLOADING -> done += item.downloadProgressProvider()
            EntryDownloadState.QUEUE -> Unit
            EntryDownloadState.NOT_DOWNLOADED, EntryDownloadState.ERROR -> continue
        }
        total += 100
    }
    return if (total == 0) 0 else done * 100 / total
}
