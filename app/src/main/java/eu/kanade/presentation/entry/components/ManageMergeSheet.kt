package eu.kanade.presentation.entry.components

import androidx.compose.runtime.Composable
import eu.kanade.tachiyomi.ui.entry.EntryViewModel
import kotlinx.collections.immutable.toPersistentList
import mihon.presentation.kit.dialog.DialogAction
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun ManageMergeDialog(
    targetId: Long,
    members: List<EntryViewModel.MergeMember>,
    removableIds: List<Long>,
    libraryRemovalIds: List<Long>,
    onDismissRequest: () -> Unit,
    onMove: (Int, Int) -> Unit,
    onSaveOrder: () -> Unit,
    onOpenManga: (Long) -> Unit,
    onSelectTarget: (Long) -> Unit,
    onToggleRemoveMember: (Long) -> Unit,
    onToggleRemoveMemberFromLibrary: (Long) -> Unit,
    onRemoveMembers: (List<Long>) -> Unit,
    onUnmergeAll: () -> Unit,
) {
    MergeEditorDialog(
        title = stringResource(MR.strings.action_manage_merge),
        entries = members.map(EntryViewModel.MergeMember::toMergeEditorEntry).toPersistentList(),
        targetId = targetId,
        targetLocked = false,
        onDismissRequest = onDismissRequest,
        onMove = onMove,
        onConfirm = onSaveOrder,
        confirmEnabled = true,
        confirmText = stringResource(MR.strings.action_save),
        removableIds = removableIds.toSet(),
        libraryRemovalIds = libraryRemovalIds.toSet(),
        onSelectTarget = onSelectTarget,
        onToggleRemove = onToggleRemoveMember,
        onToggleLibraryRemove = onToggleRemoveMemberFromLibrary,
        onOpenManga = onOpenManga,
        neutral = DialogAction(stringResource(MR.strings.action_unmerge), onUnmergeAll),
    )
}

private fun EntryViewModel.MergeMember.toMergeEditorEntry(): MergeEditorEntry {
    return MergeEditorEntry(
        id = id,
        entry = entry,
        subtitle = subtitle,
        isRemovable = true,
    )
}
