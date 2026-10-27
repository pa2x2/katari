package eu.kanade.presentation.entry.components

import androidx.compose.runtime.Composable
import eu.kanade.tachiyomi.ui.entry.merge.MergeMember
import kotlinx.collections.immutable.toPersistentList
import mihon.presentation.kit.dialog.DialogAction
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun ManageMergeSheet(
    targetId: Long,
    targetLocked: Boolean,
    lock: MergeEditorLock?,
    members: List<MergeMember>,
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
    MergeEditorSheet(
        title = stringResource(MR.strings.action_manage_merge),
        entries = members.map(MergeMember::toMergeEditorEntry).toPersistentList(),
        targetId = targetId,
        targetLocked = targetLocked,
        lock = lock,
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
        // Unmerging lets go of the members a user added, so there is nothing to unmerge when the source supplies them all
        neutral = DialogAction(stringResource(MR.strings.action_unmerge), onUnmergeAll)
            .takeIf { members.any { !it.locked } },
    )
}

private fun MergeMember.toMergeEditorEntry(): MergeEditorEntry {
    return MergeEditorEntry(
        id = id,
        entry = entry,
        subtitle = subtitle,
        isRemovable = removable,
        locked = locked,
    )
}
