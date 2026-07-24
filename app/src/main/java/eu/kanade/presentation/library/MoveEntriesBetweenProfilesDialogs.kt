package eu.kanade.presentation.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import mihon.entry.interactions.EntryProfileMoveConflict
import mihon.entry.interactions.EntryProfileMoveConflictResolution
import mihon.feature.profiles.core.Profile
import tachiyomi.domain.category.model.Category
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun MoveEntriesProfileDialog(
    profiles: List<Profile>,
    onDismissRequest: () -> Unit,
    onProfileSelected: (Profile) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(MR.strings.move_entries_select_profile)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                profiles.forEach { profile ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onProfileSelected(profile) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(profile.name, modifier = Modifier.weight(1f))
                        if (profile.requiresAuth) {
                            Icon(Icons.Outlined.Lock, contentDescription = stringResource(MR.strings.profiles_locked))
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(MR.strings.action_cancel))
            }
        },
    )
}

@Composable
fun MoveEntriesCategoryDialog(
    categories: List<Category>,
    onDismissRequest: () -> Unit,
    onCategorySelected: (Long?) -> Unit,
) {
    var selectedCategoryId by remember { mutableLongStateOf(Category.UNCATEGORIZED_ID) }
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(MR.strings.move_entries_select_category)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                CategoryChoiceRow(
                    name = stringResource(MR.strings.move_entries_uncategorized),
                    selected = selectedCategoryId == Category.UNCATEGORIZED_ID,
                    onClick = { selectedCategoryId = Category.UNCATEGORIZED_ID },
                )
                categories.forEach { category ->
                    CategoryChoiceRow(
                        name = category.name,
                        selected = selectedCategoryId == category.id,
                        onClick = { selectedCategoryId = category.id },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onCategorySelected(selectedCategoryId.takeUnless { it == Category.UNCATEGORIZED_ID })
                },
            ) {
                Text(stringResource(MR.strings.move_entries_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(MR.strings.action_cancel))
            }
        },
    )
}

@Composable
private fun CategoryChoiceRow(name: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(name)
    }
}

@Composable
fun MoveEntriesConflictDialog(
    conflict: EntryProfileMoveConflict,
    conflictNumber: Int,
    conflictCount: Int,
    destinationProfileName: String,
    sourceName: String,
    onDismissRequest: () -> Unit,
    onResolve: (EntryProfileMoveConflictResolution) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(stringResource(MR.strings.move_entries_duplicate_title, conflictNumber, conflictCount))
        },
        text = {
            Column {
                Text(conflict.sourceEntry.displayTitle)
                Text(sourceName)
                Text(conflict.sourceEntry.type.name.lowercase().replaceFirstChar(Char::titlecase))
                Text(destinationProfileName, modifier = Modifier.padding(top = 8.dp))
                if (conflict.destinationMergeAffected) {
                    Text(
                        text = stringResource(MR.strings.move_entries_merge_warning),
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
            }
        },
        confirmButton = {
            Column(horizontalAlignment = Alignment.End) {
                TextButton(onClick = { onResolve(EntryProfileMoveConflictResolution.OVERWRITE_DESTINATION) }) {
                    Text(stringResource(MR.strings.move_entries_overwrite))
                }
                TextButton(onClick = {
                    onResolve(EntryProfileMoveConflictResolution.KEEP_DESTINATION_REMOVE_SOURCE)
                }) {
                    Text(stringResource(MR.strings.move_entries_remove_current))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = { onResolve(EntryProfileMoveConflictResolution.KEEP_SOURCE) }) {
                Text(stringResource(MR.strings.move_entries_keep_source))
            }
        },
    )
}
