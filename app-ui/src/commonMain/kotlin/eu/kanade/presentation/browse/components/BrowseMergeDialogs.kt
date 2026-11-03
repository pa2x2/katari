package eu.kanade.presentation.browse.components

import androidx.compose.runtime.Composable
import eu.kanade.presentation.entry.components.MergeEditorEntry
import eu.kanade.presentation.entry.components.MergeEditorLock
import eu.kanade.presentation.entry.components.MergeEditorSheet
import eu.kanade.presentation.entry.components.MergeTarget
import eu.kanade.presentation.entry.components.MergeTargetPickerSheet
import kotlinx.collections.immutable.toPersistentList
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun MergeTargetPickerDialog(
    title: String,
    query: String,
    visibleTargets: List<MergeTarget>,
    onDismissRequest: () -> Unit,
    onQueryChange: (String) -> Unit,
    onSelectTarget: (Long) -> Unit,
) {
    MergeTargetPickerSheet(
        title = title,
        query = query,
        entries = visibleTargets.map { it.entry }.toPersistentList(),
        onDismissRequest = onDismissRequest,
        onQueryChange = onQueryChange,
        onSelectTarget = onSelectTarget,
    )
}

@Composable
fun BrowseMergeEditorSheet(
    entries: List<MergeEditorEntry>,
    targetId: Long,
    targetLocked: Boolean,
    removedIds: Set<Long>,
    libraryRemovalIds: Set<Long>,
    confirmEnabled: Boolean,
    onDismissRequest: () -> Unit,
    onMove: (Int, Int) -> Unit,
    onSelectTarget: ((Long) -> Unit)? = null,
    onToggleRemove: (Long) -> Unit,
    onToggleLibraryRemove: ((Long) -> Unit)? = null,
    onConfirm: () -> Unit,
    lock: MergeEditorLock? = null,
) {
    MergeEditorSheet(
        title = stringResource(MR.strings.action_merge),
        confirmText = stringResource(MR.strings.action_merge),
        entries = entries.toPersistentList(),
        lock = lock,
        targetId = targetId,
        targetLocked = targetLocked,
        onDismissRequest = onDismissRequest,
        onMove = onMove,
        onConfirm = onConfirm,
        removableIds = removedIds,
        libraryRemovalIds = libraryRemovalIds,
        onSelectTarget = onSelectTarget,
        onToggleRemove = onToggleRemove,
        onToggleLibraryRemove = onToggleLibraryRemove,
        confirmEnabled = confirmEnabled,
    )
}
