package eu.kanade.tachiyomi.ui.entry.merge

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import eu.kanade.presentation.browse.components.BrowseMergeEditorSheet
import eu.kanade.presentation.browse.components.MergeTargetPickerDialog
import eu.kanade.presentation.entry.components.ManageMergeSheet
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** Shows [model]'s merge sheet; [onOpenEntry] opens a member, on its own when it is not the merged entry. */
@Composable
fun EntryMergeSheets(
    model: EntryMergeModel,
    onOpenEntry: (entryId: Long, bypassMerge: Boolean) -> Unit,
) {
    val current by model.sheet.collectAsStateWithLifecycle()
    val onDismissMergeSheet = { model.dismiss() }
    when (val sheet = current) {
        null -> {}
        is EntryMergeSheet.SelectTarget -> {
            MergeTargetPickerDialog(
                title = stringResource(MR.strings.action_merge_into_library),
                query = sheet.query,
                visibleTargets = sheet.visibleTargets,
                onDismissRequest = onDismissMergeSheet,
                onQueryChange = model::updateTargetQuery,
                onSelectTarget = model::openEditor,
            )
        }
        is EntryMergeSheet.Edit -> {
            BrowseMergeEditorSheet(
                entries = sheet.entries,
                targetId = sheet.targetId,
                targetLocked = sheet.targetLocked,
                lock = sheet.lock,
                removedIds = sheet.removedIds,
                libraryRemovalIds = sheet.libraryRemovalIds,
                confirmEnabled = sheet.enabled,
                onDismissRequest = onDismissMergeSheet,
                onMove = model::moveEditorEntry,
                onSelectTarget = model::setEditorTarget,
                onToggleRemove = model::toggleEditorRemoval,
                onToggleLibraryRemove = model::toggleEditorLibraryRemoval,
                onConfirm = model::confirmEditor,
            )
        }
        is EntryMergeSheet.Manage -> {
            ManageMergeSheet(
                targetId = sheet.targetId,
                targetLocked = sheet.targetLocked,
                lock = sheet.lock,
                members = sheet.members,
                removableIds = sheet.removableIds,
                libraryRemovalIds = sheet.libraryRemovalIds,
                onDismissRequest = onDismissMergeSheet,
                onMove = model::reorderMembers,
                onSaveOrder = model::saveOrder,
                onOpenManga = { entryIdToOpen ->
                    model.dismiss()
                    onOpenEntry(entryIdToOpen, entryIdToOpen != sheet.savedTargetId)
                },
                onSelectTarget = model::setManageTarget,
                onToggleRemoveMember = model::toggleMemberRemoval,
                onToggleRemoveMemberFromLibrary = model::toggleMemberLibraryRemoval,
                onRemoveMembers = model::removeMembers,
                onUnmergeAll = model::unmergeAll,
            )
        }
    }
}
