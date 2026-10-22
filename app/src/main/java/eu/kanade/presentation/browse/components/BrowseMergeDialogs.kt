package eu.kanade.presentation.browse.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import eu.kanade.presentation.entry.components.EntryActionIcons
import eu.kanade.presentation.entry.components.MergeEditorEntry
import eu.kanade.presentation.entry.components.MergeEditorSheet
import eu.kanade.presentation.entry.components.MergeTarget
import eu.kanade.presentation.entry.components.MergeTargetPickerSheet
import kotlinx.collections.immutable.toPersistentList
import mihon.presentation.kit.button.Button
import mihon.presentation.kit.button.ButtonStyle
import mihon.presentation.kit.button.StackedActionButton
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.sheet.Sheet
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun BrowseLibraryActionDialog(
    mangaTitle: String,
    favorite: Boolean,
    onDismissRequest: () -> Unit,
    onLibraryAction: () -> Unit,
    onMergeIntoLibrary: () -> Unit,
) {
    val spacing = KatariTheme.spacing
    Sheet(onDismissRequest = onDismissRequest) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = spacing.l),
            verticalArrangement = Arrangement.spacedBy(spacing.l),
        ) {
            Text(
                text = mangaTitle,
                modifier = Modifier.padding(horizontal = spacing.rowInset),
                role = TextRole.SheetTitle,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.rowInset),
                horizontalArrangement = Arrangement.spacedBy(spacing.s),
            ) {
                StackedActionButton(
                    label = stringResource(if (favorite) MR.strings.in_library else MR.strings.add_to_library),
                    icon = EntryActionIcons.library(favorite),
                    onClick = onLibraryAction,
                    modifier = Modifier.weight(1f),
                )
                StackedActionButton(
                    label = stringResource(MR.strings.action_merge_into_library),
                    icon = EntryActionIcons.merge,
                    onClick = onMergeIntoLibrary,
                    modifier = Modifier.weight(1f),
                )
            }
            Button(
                label = stringResource(MR.strings.action_cancel),
                onClick = onDismissRequest,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.rowInset),
                style = ButtonStyle.Text,
            )
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
) {
    MergeEditorSheet(
        title = stringResource(MR.strings.action_merge),
        confirmText = stringResource(MR.strings.action_merge),
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
