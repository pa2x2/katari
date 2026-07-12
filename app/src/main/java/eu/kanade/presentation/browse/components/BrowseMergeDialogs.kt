package eu.kanade.presentation.browse.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.CallSplit
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.components.AdaptiveSheet
import eu.kanade.presentation.entry.components.MergeEditorDialog
import eu.kanade.presentation.entry.components.MergeEditorEntry
import eu.kanade.presentation.entry.components.MergeTarget
import eu.kanade.presentation.entry.components.MergeTargetPickerSheet
import kotlinx.collections.immutable.toPersistentList
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.ActionButton
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun BrowseLibraryActionDialog(
    mangaTitle: String,
    favorite: Boolean,
    onDismissRequest: () -> Unit,
    onLibraryAction: () -> Unit,
    onMergeIntoLibrary: () -> Unit,
) {
    AdaptiveSheet(onDismissRequest = onDismissRequest) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = MaterialTheme.padding.medium),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.medium),
        ) {
            Text(
                text = mangaTitle,
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
            ) {
                ActionButton(
                    modifier = Modifier.weight(1f),
                    title = stringResource(if (favorite) MR.strings.in_library else MR.strings.add_to_library),
                    icon = if (favorite) Icons.Outlined.Remove else Icons.Outlined.Add,
                    onClick = {
                        onLibraryAction()
                    },
                )
                ActionButton(
                    modifier = Modifier.weight(1f),
                    title = stringResource(MR.strings.action_merge_into_library),
                    icon = Icons.AutoMirrored.Outlined.CallSplit,
                    onClick = onMergeIntoLibrary,
                )
            }
            HorizontalDivider()
            TextButton(
                onClick = onDismissRequest,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
            ) {
                Text(text = stringResource(MR.strings.action_cancel))
            }
        }
    }
}

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
fun BrowseMergeEditorDialog(
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
) {
    MergeEditorDialog(
        title = stringResource(MR.strings.action_merge),
        entries = entries.toPersistentList(),
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
