package eu.kanade.presentation.library

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import app.katari.entry.interactions.lifecycle.profile.EntryProfileMoveConflict
import app.katari.entry.interactions.lifecycle.profile.EntryProfileMoveConflictResolution
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.rounded.Lock
import app.katari.presentation.kit.content.Icon
import app.katari.presentation.kit.content.Text
import app.katari.presentation.kit.dialog.Dialog
import app.katari.presentation.kit.dialog.DialogAction
import app.katari.presentation.kit.list.ChoiceRow
import app.katari.presentation.kit.list.SettingsRow
import app.katari.presentation.kit.theme.KatariTheme
import app.katari.profiles.core.Profile
import tachiyomi.domain.category.model.Category
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun MoveEntriesProfileDialog(
    profiles: List<Profile>,
    onDismissRequest: () -> Unit,
    onProfileSelected: (Profile) -> Unit,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        title = stringResource(MR.strings.move_entries_select_profile),
        confirm = null,
        dismiss = DialogAction(stringResource(MR.strings.action_cancel), onDismissRequest),
    ) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            profiles.forEach { profile ->
                SettingsRow(
                    title = profile.name,
                    trailing = if (profile.requiresAuth) {
                        {
                            Icon(
                                MaterialSymbols.Rounded.Lock,
                                contentDescription = stringResource(MR.strings.profiles_locked),
                            )
                        }
                    } else {
                        null
                    },
                    onClick = { onProfileSelected(profile) },
                )
            }
        }
    }
}

@Composable
fun MoveEntriesCategoryDialog(
    categories: List<Category>,
    onDismissRequest: () -> Unit,
    onCategorySelected: (Long?) -> Unit,
) {
    var selectedCategoryId by remember { mutableLongStateOf(Category.UNCATEGORIZED_ID) }
    Dialog(
        onDismissRequest = onDismissRequest,
        title = stringResource(MR.strings.move_entries_select_category),
        confirm = DialogAction(
            label = stringResource(MR.strings.move_entries_confirm),
            onClick = { onCategorySelected(selectedCategoryId.takeUnless { it == Category.UNCATEGORIZED_ID }) },
        ),
        dismiss = DialogAction(stringResource(MR.strings.action_cancel), onDismissRequest),
    ) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            ChoiceRow(
                title = stringResource(MR.strings.move_entries_uncategorized),
                selected = selectedCategoryId == Category.UNCATEGORIZED_ID,
                onClick = { selectedCategoryId = Category.UNCATEGORIZED_ID },
            )
            categories.forEach { category ->
                ChoiceRow(
                    title = category.name,
                    selected = selectedCategoryId == category.id,
                    onClick = { selectedCategoryId = category.id },
                )
            }
        }
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
    Dialog(
        onDismissRequest = onDismissRequest,
        title = stringResource(MR.strings.move_entries_duplicate_title, conflictNumber, conflictCount),
        confirm = DialogAction(
            label = stringResource(MR.strings.move_entries_overwrite),
            onClick = { onResolve(EntryProfileMoveConflictResolution.OVERWRITE_DESTINATION) },
        ),
        neutral = DialogAction(
            label = stringResource(MR.strings.move_entries_remove_current),
            onClick = { onResolve(EntryProfileMoveConflictResolution.KEEP_DESTINATION_REMOVE_SOURCE) },
        ),
        dismiss = DialogAction(
            label = stringResource(MR.strings.move_entries_keep_source),
            onClick = { onResolve(EntryProfileMoveConflictResolution.KEEP_SOURCE) },
        ),
    ) {
        Column {
            Text(conflict.sourceEntry.displayTitle)
            Text(sourceName)
            Text(conflict.sourceEntry.type.name.lowercase().replaceFirstChar(Char::titlecase))
            Text(destinationProfileName, modifier = Modifier.padding(top = KatariTheme.spacing.s))
            if (conflict.destinationMergeAffected) {
                Text(
                    text = stringResource(MR.strings.move_entries_merge_warning),
                    modifier = Modifier.padding(top = KatariTheme.spacing.m),
                )
            }
        }
    }
}
