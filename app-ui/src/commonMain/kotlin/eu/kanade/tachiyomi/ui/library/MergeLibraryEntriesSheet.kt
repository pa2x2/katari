package eu.kanade.tachiyomi.ui.library

import androidx.compose.runtime.Composable
import eu.kanade.presentation.entry.components.MergeEditorEntry
import eu.kanade.presentation.entry.components.MergeEditorSheet
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun MergeLibraryEntriesSheet(
    dialog: LibraryViewModel.Dialog.MergeEntry,
    onDismissRequest: () -> Unit,
    onMove: (Int, Int) -> Unit,
    onSelectTarget: (Long) -> Unit,
    onConfirm: () -> Unit,
) {
    val entries: PersistentList<MergeEditorEntry> = dialog.entries
        .map(LibraryViewModel.MergeEntry::toMergeEditorEntry)
        .toPersistentList()

    MergeEditorSheet(
        title = stringResource(MR.strings.action_merge),
        confirmText = stringResource(MR.strings.action_merge),
        entries = entries,
        targetId = dialog.targetId,
        targetLocked = dialog.targetLocked,
        lock = dialog.lock,
        onDismissRequest = onDismissRequest,
        onMove = onMove,
        onConfirm = onConfirm,
        onSelectTarget = onSelectTarget.takeUnless { dialog.targetLocked },
    )
}

private fun LibraryViewModel.MergeEntry.toMergeEditorEntry(): MergeEditorEntry {
    return MergeEditorEntry(
        id = id,
        entry = entry,
        subtitle = subtitle,
        isMember = isFromExistingMerge,
        locked = locked,
    )
}
