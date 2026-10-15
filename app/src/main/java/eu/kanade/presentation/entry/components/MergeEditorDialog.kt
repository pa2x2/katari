package eu.kanade.presentation.entry.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import eu.kanade.presentation.components.GuideButton
import kotlinx.collections.immutable.ImmutableList
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.DragHandle
import mihon.icons.materialsymbols.rounded.Lock
import mihon.icons.materialsymbols.rounded.MoreVert
import mihon.icons.materialsymbols.roundedfilled.CheckCircle
import mihon.presentation.kit.button.IconButton
import mihon.presentation.kit.content.Badge
import mihon.presentation.kit.content.BadgeGroup
import mihon.presentation.kit.content.BadgeTone
import mihon.presentation.kit.content.EntryMetrics
import mihon.presentation.kit.content.Icon
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.control.RadioButton
import mihon.presentation.kit.dialog.Dialog
import mihon.presentation.kit.dialog.DialogAction
import mihon.presentation.kit.menu.DropdownMenu
import mihon.presentation.kit.menu.DropdownMenuItem
import mihon.presentation.kit.surface.Surface
import mihon.presentation.kit.surface.SurfaceContainer
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import sh.calvin.reorderable.ReorderableCollectionItemScope
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Edits a merge before it is saved: members are reordered by dragging, and each can be made the merge's root, removed
 * from the merge or from the library. [neutral] adds an action before Cancel, such as unmerging everything.
 */
@Composable
fun MergeEditorDialog(
    title: String,
    entries: ImmutableList<MergeEditorEntry>,
    targetId: Long,
    targetLocked: Boolean,
    onDismissRequest: () -> Unit,
    onMove: (Int, Int) -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    confirmEnabled: Boolean = entries.size >= 2,
    confirmText: String = stringResource(MR.strings.action_ok),
    summaryText: String = stringResource(MR.strings.merge_editor_summary),
    removableIds: Set<Long> = emptySet(),
    libraryRemovalIds: Set<Long> = emptySet(),
    onSelectTarget: ((Long) -> Unit)? = null,
    onToggleRemove: ((Long) -> Unit)? = null,
    onToggleLibraryRemove: ((Long) -> Unit)? = null,
    onOpenManga: ((Long) -> Unit)? = null,
    neutral: DialogAction? = null,
) {
    val listState = rememberLazyListState()
    val reorderableState = rememberReorderableLazyListState(listState, PaddingValues()) { from, to ->
        val fromIndex = entries.indexOfFirst { it.id == from.key }
        val toIndex = entries.indexOfFirst { it.id == to.key }
        if (fromIndex == -1 || toIndex == -1) return@rememberReorderableLazyListState
        onMove(fromIndex, toIndex)
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        title = title,
        confirm = DialogAction(confirmText, onConfirm, enabled = confirmEnabled),
        modifier = modifier,
        dismiss = DialogAction(stringResource(MR.strings.action_cancel), onDismissRequest),
        neutral = neutral,
        titleAction = { GuideButton(page = "merged-entries/") },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s)) {
            Text(text = summaryText, role = TextRole.Body, tone = ContentTone.Secondary)
            LazyColumn(
                modifier = Modifier.heightIn(max = EntryMetrics.mergeEditorListMaxHeight),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s),
            ) {
                items(entries, key = { it.id }) { entry ->
                    ReorderableItem(reorderableState, entry.id, enabled = entries.size > 1) {
                        MergeEditorItem(
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
            if (targetLocked) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(imageVector = MaterialSymbols.Rounded.Lock, contentDescription = null)
                    Text(text = stringResource(MR.strings.merge_existing_target_locked))
                }
            }
        }
    }
}

@Composable
private fun ReorderableCollectionItemScope.MergeEditorItem(
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
    val hasOverflowActions =
        (onSelectTarget != null && !targetLocked && !isTarget) ||
            (canRemoveEntry && onToggleRemove != null) ||
            (canRemoveEntry && onToggleLibraryRemove != null) ||
            onOpenManga != null
    val container = when {
        markedForLibraryRemoval -> SurfaceContainer.ErrorContainer
        markedForRemoval -> SurfaceContainer.SecondaryContainer
        else -> SurfaceContainer.Low
    }
    // A marked row's container sets the colour of its secondary text; Original keeps that instead of the variant.
    val markedTone = if (markedForRemoval || markedForLibraryRemoval) ContentTone.Original else ContentTone.Inherit

    Surface(container = container, shape = KatariTheme.shapes.medium, outlined = true) {
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(KatariTheme.spacing.s),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s),
            ) {
                MergeEditorEntryCover(entry)
                MergeEditorEntryDetails(
                    entry = entry,
                    modifier = Modifier.weight(1f),
                    subtitleTone = markedTone,
                ) {
                    if (markedForRemoval) {
                        Text(text = stringResource(MR.strings.action_remove), role = TextRole.Badge)
                    }
                    if (markedForLibraryRemoval) {
                        Text(text = stringResource(MR.strings.remove_from_library), role = TextRole.Badge)
                    }
                }

                when {
                    hasOverflowActions -> MergeEditorItemMenu(
                        entry = entry,
                        isTarget = isTarget,
                        targetLocked = targetLocked,
                        canRemoveEntry = canRemoveEntry,
                        markedForRemoval = markedForRemoval,
                        markedForLibraryRemoval = markedForLibraryRemoval,
                        onSelectTarget = onSelectTarget,
                        onToggleRemove = onToggleRemove,
                        onToggleLibraryRemove = onToggleLibraryRemove,
                        onOpenManga = onOpenManga,
                    )
                    onSelectTarget != null && !targetLocked -> {
                        RadioButton(
                            selected = isTarget,
                            onClick = { onSelectTarget(entry.id) },
                        )
                    }
                    isTarget -> {
                        Icon(
                            imageVector = MaterialSymbols.RoundedFilled.CheckCircle,
                            contentDescription = null,
                            tone = ContentTone.Primary,
                        )
                    }
                }

                Icon(
                    imageVector = MaterialSymbols.Rounded.DragHandle,
                    contentDescription = null,
                    modifier = Modifier.draggableHandle(),
                    tone = ContentTone.Secondary,
                )
            }

            if (isTarget) {
                BadgeGroup(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(KatariTheme.spacing.xs),
                ) {
                    Badge(text = stringResource(MR.strings.label_target), tone = BadgeTone.Primary)
                }
            }
        }
    }
}

@Composable
private fun MergeEditorItemMenu(
    entry: MergeEditorEntry,
    isTarget: Boolean,
    targetLocked: Boolean,
    canRemoveEntry: Boolean,
    markedForRemoval: Boolean,
    markedForLibraryRemoval: Boolean,
    onSelectTarget: ((Long) -> Unit)?,
    onToggleRemove: ((Long) -> Unit)?,
    onToggleLibraryRemove: ((Long) -> Unit)?,
    onOpenManga: ((Long) -> Unit)?,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(
            icon = MaterialSymbols.Rounded.MoreVert,
            contentDescription = stringResource(MR.strings.label_more),
            onClick = { expanded = true },
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            if (onSelectTarget != null && !targetLocked && !isTarget) {
                DropdownMenuItem(
                    label = stringResource(MR.strings.action_set_as_root),
                    onClick = {
                        onSelectTarget(entry.id)
                        expanded = false
                    },
                )
            }
            if (canRemoveEntry && onToggleRemove != null) {
                DropdownMenuItem(
                    label = stringResource(if (markedForRemoval) MR.strings.action_keep else MR.strings.action_remove),
                    onClick = {
                        onToggleRemove(entry.id)
                        expanded = false
                    },
                )
            }
            if (canRemoveEntry && onToggleLibraryRemove != null) {
                DropdownMenuItem(
                    label = stringResource(
                        if (markedForLibraryRemoval) {
                            MR.strings.action_keep_in_library
                        } else {
                            MR.strings.remove_from_library
                        },
                    ),
                    onClick = {
                        onToggleLibraryRemove(entry.id)
                        expanded = false
                    },
                )
            }
            if (onOpenManga != null) {
                DropdownMenuItem(
                    label = stringResource(
                        if (isTarget) MR.strings.action_open_merged_entry else MR.strings.action_open,
                    ),
                    onClick = {
                        onOpenManga(entry.id)
                        expanded = false
                    },
                )
            }
        }
    }
}
