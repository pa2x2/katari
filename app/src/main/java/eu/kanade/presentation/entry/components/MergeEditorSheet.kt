package eu.kanade.presentation.entry.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import dev.icerock.moko.resources.StringResource
import eu.kanade.presentation.components.GuideButton
import kotlinx.collections.immutable.ImmutableList
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.automirroredrounded.OpenInNew
import mihon.icons.materialsymbols.rounded.BookmarkAdd
import mihon.icons.materialsymbols.rounded.BookmarkRemove
import mihon.icons.materialsymbols.rounded.CallSplit
import mihon.icons.materialsymbols.rounded.DragHandle
import mihon.icons.materialsymbols.rounded.Lock
import mihon.icons.materialsymbols.rounded.LockOpen
import mihon.icons.materialsymbols.rounded.Star
import mihon.icons.materialsymbols.rounded.Undo
import mihon.presentation.kit.content.Badge
import mihon.presentation.kit.content.BadgeTone
import mihon.presentation.kit.content.CoverFrame
import mihon.presentation.kit.content.Icon
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.dialog.DialogAction
import mihon.presentation.kit.list.CoverRow
import mihon.presentation.kit.list.RowGroupGap
import mihon.presentation.kit.list.RowGroupItem
import mihon.presentation.kit.list.RowGroupPosition
import mihon.presentation.kit.menu.DropdownMenuDivider
import mihon.presentation.kit.menu.DropdownMenuItem
import mihon.presentation.kit.menu.OverflowMenu
import mihon.presentation.kit.sheet.Sheet
import mihon.presentation.kit.sheet.SheetActions
import mihon.presentation.kit.sheet.SheetHeader
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import sh.calvin.reorderable.ReorderableCollectionItemScope
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Edits a merge before it is saved: members are one card of rows in merge order, reordered by dragging their handle.
 * The root's row is filled; every other action on a member, making it the root included, is in its ⋮ menu. [neutral]
 * adds an action at the start of the buttons, such as unmerging everything.
 *
 * In a merged entry's merge, the members its source supplies are locked: they show a lock in place of the handle and
 * have no menu, and nothing can be moved above the locked root. [lock] names the source under the members and lists the
 * titles another merge holds greyed in a card of their own.
 */
@Composable
fun MergeEditorSheet(
    title: String,
    entries: ImmutableList<MergeEditorEntry>,
    targetId: Long,
    targetLocked: Boolean,
    onDismissRequest: () -> Unit,
    onMove: (Int, Int) -> Unit,
    onConfirm: () -> Unit,
    confirmText: String,
    modifier: Modifier = Modifier,
    confirmEnabled: Boolean = entries.size >= 2,
    summaryText: String = stringResource(MR.strings.merge_editor_summary),
    removableIds: Set<Long> = emptySet(),
    libraryRemovalIds: Set<Long> = emptySet(),
    onSelectTarget: ((Long) -> Unit)? = null,
    onToggleRemove: ((Long) -> Unit)? = null,
    onToggleLibraryRemove: ((Long) -> Unit)? = null,
    onOpenManga: ((Long) -> Unit)? = null,
    neutral: DialogAction? = null,
    lock: MergeEditorLock? = null,
) {
    val listState = rememberLazyListState()
    val reorderableState = rememberReorderableLazyListState(listState, PaddingValues()) { from, to ->
        val fromIndex = entries.indexOfFirst { it.id == from.key }
        val toIndex = entries.indexOfFirst { it.id == to.key }
        if (fromIndex == -1 || toIndex == -1) return@rememberReorderableLazyListState
        if (entries[fromIndex].locked || (targetLocked && entries[toIndex].id == targetId)) {
            return@rememberReorderableLazyListState
        }
        onMove(fromIndex, toIndex)
    }

    // Edits are only kept by the buttons, so a stray swipe or tap outside must not drop them.
    Sheet(onDismissRequest = onDismissRequest, modifier = modifier, enableImplicitDismiss = false) {
        Column {
            SheetHeader(title = title, actions = { GuideButton(page = "merged-entries/") })
            Text(
                text = summaryText,
                modifier = Modifier.padding(
                    start = KatariTheme.spacing.rowInset,
                    end = KatariTheme.spacing.rowInset,
                    bottom = KatariTheme.spacing.m,
                ),
                role = TextRole.Body,
                tone = ContentTone.Secondary,
            )
            LazyColumn(modifier = Modifier.weight(1f, fill = false), state = listState) {
                itemsIndexed(entries, key = { _, entry -> entry.id }) { index, entry ->
                    // Other rows pass locked members but never the locked root
                    val movable = entries.size > 1 && !(targetLocked && entry.id == targetId)
                    ReorderableItem(reorderableState, entry.id, enabled = movable) { isDragging ->
                        RowGroupItem(
                            position = RowGroupPosition.of(index, entries.size),
                            modifier = Modifier.animateItem(),
                            selected = entry.id == targetId,
                            lifted = isDragging,
                        ) {
                            MergeEditorRow(
                                entry = entry,
                                isTarget = entry.id == targetId,
                                targetLocked = targetLocked,
                                markedForRemoval = entry.id in removableIds,
                                markedForLibraryRemoval = entry.id in libraryRemovalIds,
                                onSelectTarget = onSelectTarget,
                                onToggleRemove = onToggleRemove,
                                onToggleLibraryRemove = onToggleLibraryRemove,
                                onOpenManga = onOpenManga,
                            )
                        }
                    }
                }
                val takenEntries = lock?.takenEntries.orEmpty()
                if (takenEntries.isNotEmpty()) {
                    item(key = "taken-gap") { RowGroupGap() }
                    itemsIndexed(takenEntries, key = { _, entry -> "taken-${entry.id}" }) { index, entry ->
                        RowGroupItem(position = RowGroupPosition.of(index, takenEntries.size)) {
                            TakenEntryRow(entry)
                        }
                    }
                }
            }
            if (lock != null) {
                Row(
                    modifier = Modifier.padding(
                        start = KatariTheme.spacing.rowInset,
                        end = KatariTheme.spacing.rowInset,
                        top = KatariTheme.spacing.m,
                    ),
                    horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = MaterialSymbols.Rounded.Lock,
                        contentDescription = null,
                        tone = ContentTone.Secondary,
                    )
                    Text(
                        text = stringResource(MR.strings.merge_editor_locked_note, lock.sourceName),
                        role = TextRole.Body,
                        tone = ContentTone.Secondary,
                    )
                }
            }
            SheetActions(
                confirm = DialogAction(confirmText, onConfirm, enabled = confirmEnabled),
                dismiss = DialogAction(stringResource(MR.strings.action_cancel), onDismissRequest),
                neutral = neutral,
            )
        }
    }
}

@Composable
private fun ReorderableCollectionItemScope.MergeEditorRow(
    entry: MergeEditorEntry,
    isTarget: Boolean,
    targetLocked: Boolean,
    markedForRemoval: Boolean,
    markedForLibraryRemoval: Boolean,
    onSelectTarget: ((Long) -> Unit)?,
    onToggleRemove: ((Long) -> Unit)?,
    onToggleLibraryRemove: ((Long) -> Unit)?,
    onOpenManga: ((Long) -> Unit)?,
) {
    val canRemoveEntry = entry.isRemovable && !isTarget
    val canSetRoot = onSelectTarget != null && !targetLocked && !isTarget
    val marked = markedForRemoval || markedForLibraryRemoval
    CoverRow(
        cover = {
            EntryCover.Book(modifier = Modifier.fillMaxSize(), data = entry.coverData, frame = CoverFrame.Thumbnail)
        },
        leading = {
            if (entry.locked) {
                Icon(
                    imageVector = MaterialSymbols.Rounded.Lock,
                    contentDescription = stringResource(MR.strings.merge_editor_locked),
                    tone = ContentTone.Secondary,
                )
            } else {
                Icon(
                    imageVector = MaterialSymbols.Rounded.DragHandle,
                    contentDescription = null,
                    modifier = Modifier.draggableHandle(),
                    tone = ContentTone.Secondary,
                )
            }
        },
        trailing = if (entry.locked) {
            null
        } else {
            {
                MergeEditorRowMenu(
                    entry = entry,
                    isTarget = isTarget,
                    canSetRoot = canSetRoot,
                    canRemoveEntry = canRemoveEntry,
                    markedForRemoval = markedForRemoval,
                    markedForLibraryRemoval = markedForLibraryRemoval,
                    onSelectTarget = onSelectTarget,
                    onToggleRemove = onToggleRemove,
                    onToggleLibraryRemove = onToggleLibraryRemove,
                    onOpenManga = onOpenManga,
                )
            }
        },
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = entry.title,
                modifier = Modifier.weight(1f, fill = false),
                role = TextRole.ItemTitle,
                tone = if (marked) ContentTone.Disabled else ContentTone.Inherit,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (isTarget) Badge(text = stringResource(MR.strings.label_target), tone = BadgeTone.Primary)
        }
        when {
            markedForLibraryRemoval -> MarkedLine(MR.strings.merge_editor_marked_library_remove)
            markedForRemoval -> MarkedLine(MR.strings.merge_editor_marked_remove)
            else -> Text(
                text = mergeEditorSubtitle(entry),
                role = TextRole.Supporting,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun TakenEntryRow(entry: MergeEditorEntry) {
    CoverRow(
        cover = {
            EntryCover.Book(modifier = Modifier.fillMaxSize(), data = entry.coverData, frame = CoverFrame.Thumbnail)
        },
        leading = {
            Icon(imageVector = MaterialSymbols.Rounded.LockOpen, contentDescription = null, tone = ContentTone.Disabled)
        },
    ) {
        Text(
            text = entry.title,
            role = TextRole.ItemTitle,
            tone = ContentTone.Disabled,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = stringResource(MR.strings.merge_editor_taken),
            role = TextRole.Supporting,
            tone = ContentTone.Disabled,
            maxLines = 1,
        )
    }
}

@Composable
private fun MarkedLine(text: StringResource) {
    Text(text = stringResource(text), role = TextRole.Supporting, tone = ContentTone.Error)
}

@Composable
private fun MergeEditorRowMenu(
    entry: MergeEditorEntry,
    isTarget: Boolean,
    canSetRoot: Boolean,
    canRemoveEntry: Boolean,
    markedForRemoval: Boolean,
    markedForLibraryRemoval: Boolean,
    onSelectTarget: ((Long) -> Unit)?,
    onToggleRemove: ((Long) -> Unit)?,
    onToggleLibraryRemove: ((Long) -> Unit)?,
    onOpenManga: ((Long) -> Unit)?,
) {
    val removal = canRemoveEntry && (onToggleRemove != null || onToggleLibraryRemove != null)
    if (!canSetRoot && !removal && onOpenManga == null) return
    OverflowMenu { _ ->
        if (canSetRoot && onSelectTarget != null) {
            DropdownMenuItem(
                label = stringResource(MR.strings.action_set_as_root),
                onClick = { onSelectTarget(entry.id) },
                icon = MaterialSymbols.Rounded.Star,
            )
        }
        if (onOpenManga != null) {
            DropdownMenuItem(
                label = stringResource(
                    if (isTarget) MR.strings.action_open_merged_entry else MR.strings.action_open,
                ),
                onClick = { onOpenManga(entry.id) },
                icon = MaterialSymbols.AutoMirroredRounded.OpenInNew,
            )
        }
        if (removal) {
            if (canSetRoot || onOpenManga != null) DropdownMenuDivider()
            if (onToggleRemove != null) {
                DropdownMenuItem(
                    label = stringResource(
                        if (markedForRemoval) MR.strings.action_keep else MR.strings.action_remove,
                    ),
                    onClick = { onToggleRemove(entry.id) },
                    icon = if (markedForRemoval) MaterialSymbols.Rounded.Undo else MaterialSymbols.Rounded.CallSplit,
                )
            }
            if (onToggleLibraryRemove != null) {
                DropdownMenuItem(
                    label = stringResource(
                        if (markedForLibraryRemoval) {
                            MR.strings.action_keep_in_library
                        } else {
                            MR.strings.remove_from_library
                        },
                    ),
                    onClick = { onToggleLibraryRemove(entry.id) },
                    icon = if (markedForLibraryRemoval) {
                        MaterialSymbols.Rounded.BookmarkAdd
                    } else {
                        MaterialSymbols.Rounded.BookmarkRemove
                    },
                )
            }
        }
    }
}
