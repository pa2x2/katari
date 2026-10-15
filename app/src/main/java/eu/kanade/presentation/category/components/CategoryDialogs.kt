package eu.kanade.presentation.category.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.semantics.Role
import eu.kanade.core.preference.asToggleableState
import eu.kanade.presentation.category.visualName
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.control.Checkbox
import mihon.presentation.kit.control.TriStateCheckbox
import mihon.presentation.kit.dialog.ConfirmDialog
import mihon.presentation.kit.dialog.Dialog
import mihon.presentation.kit.dialog.DialogAction
import mihon.presentation.kit.field.TextField
import mihon.presentation.kit.list.SettingsRow
import mihon.presentation.kit.list.ToggleRow
import tachiyomi.core.common.preference.CheckboxState
import tachiyomi.domain.category.model.Category
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun CategoryCreateDialog(
    onDismissRequest: () -> Unit,
    onCreate: (String) -> Unit,
    categories: List<String>,
) {
    var name by remember { mutableStateOf("") }

    val focusRequester = remember { FocusRequester() }
    val nameAlreadyExists = remember(name) { categories.contains(name) }

    Dialog(
        onDismissRequest = onDismissRequest,
        title = stringResource(MR.strings.action_add_category),
        confirm = DialogAction(
            label = stringResource(MR.strings.action_add),
            onClick = {
                onCreate(name)
                onDismissRequest()
            },
            enabled = name.isNotEmpty() && !nameAlreadyExists,
        ),
        dismiss = DialogAction(stringResource(MR.strings.action_cancel), onDismissRequest),
    ) {
        TextField(
            value = name,
            onValueChange = { name = it },
            modifier = Modifier.focusRequester(focusRequester),
            label = stringResource(MR.strings.name),
            supportingText = stringResource(
                if (name.isNotEmpty() && nameAlreadyExists) {
                    MR.strings.error_category_exists
                } else {
                    MR.strings.information_required_plain
                },
            ),
            isError = name.isNotEmpty() && nameAlreadyExists,
        )
    }

    LaunchedEffect(focusRequester) {
        focusRequester.requestFocus()
    }
}

/** Renames a category and switches whether library updates check it, applying both on confirm. */
@Composable
fun CategoryEditDialog(
    onDismissRequest: () -> Unit,
    onConfirm: (name: String, autoUpdate: Boolean) -> Unit,
    categories: List<String>,
    category: String,
    autoUpdate: Boolean,
) {
    var name by remember { mutableStateOf(category) }
    var checked by remember { mutableStateOf(autoUpdate) }
    val nameHasChanged = name != category

    val focusRequester = remember { FocusRequester() }
    val nameAlreadyExists = remember(name) { categories.contains(name) }

    Dialog(
        onDismissRequest = onDismissRequest,
        title = stringResource(MR.strings.action_edit_category),
        confirm = DialogAction(
            label = stringResource(MR.strings.action_ok),
            onClick = {
                onConfirm(name, checked)
                onDismissRequest()
            },
            enabled = (nameHasChanged || checked != autoUpdate) &&
                name.isNotEmpty() &&
                !(nameHasChanged && nameAlreadyExists),
        ),
        dismiss = DialogAction(stringResource(MR.strings.action_cancel), onDismissRequest),
    ) {
        Column {
            TextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.focusRequester(focusRequester),
                label = stringResource(MR.strings.name),
                supportingText = stringResource(
                    if (nameHasChanged && nameAlreadyExists) {
                        MR.strings.error_category_exists
                    } else {
                        MR.strings.information_required_plain
                    },
                ),
                isError = nameHasChanged && nameAlreadyExists,
            )
            ToggleRow(
                title = stringResource(MR.strings.library_updates_check_automatically),
                checked = checked,
                onCheckedChange = { checked = it },
                subtitle = if (checked) null else stringResource(MR.strings.library_updates_category_off_summary),
            )
        }
    }

    LaunchedEffect(focusRequester) {
        focusRequester.requestFocus()
    }
}

@Composable
fun CategoryDeleteDialog(
    onDismissRequest: () -> Unit,
    onDelete: () -> Unit,
    category: String,
) {
    ConfirmDialog(
        title = stringResource(MR.strings.delete_category),
        text = stringResource(MR.strings.delete_category_confirmation, category),
        confirmLabel = stringResource(MR.strings.action_ok),
        onConfirm = {
            onDelete()
            onDismissRequest()
        },
        onDismissRequest = onDismissRequest,
        destructive = true,
    )
}

@Composable
fun ChangeCategoryDialog(
    initialSelection: List<CheckboxState<Category>>,
    onDismissRequest: () -> Unit,
    onEditCategories: () -> Unit,
    onConfirm: (List<Long>, List<Long>) -> Unit,
) {
    if (initialSelection.isEmpty()) {
        Dialog(
            onDismissRequest = onDismissRequest,
            title = stringResource(MR.strings.action_move_category),
            confirm = DialogAction(
                label = stringResource(MR.strings.action_edit_categories),
                onClick = {
                    onDismissRequest()
                    onEditCategories()
                },
            ),
        ) {
            Text(text = stringResource(MR.strings.information_empty_category_dialog))
        }
        return
    }
    var selection by remember { mutableStateOf(initialSelection) }
    Dialog(
        onDismissRequest = onDismissRequest,
        title = stringResource(MR.strings.action_move_category),
        confirm = DialogAction(
            label = stringResource(MR.strings.action_ok),
            onClick = {
                onDismissRequest()
                onConfirm(
                    selection
                        .filter { it is CheckboxState.State.Checked || it is CheckboxState.TriState.Include }
                        .map { it.value.id },
                    selection
                        .filter { it is CheckboxState.State.None || it is CheckboxState.TriState.None }
                        .map { it.value.id },
                )
            },
        ),
        dismiss = DialogAction(stringResource(MR.strings.action_cancel), onDismissRequest),
        neutral = DialogAction(
            label = stringResource(MR.strings.action_edit),
            onClick = {
                onDismissRequest()
                onEditCategories()
            },
        ),
    ) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            selection.forEach { checkbox ->
                val onChange = {
                    val index = selection.indexOf(checkbox)
                    if (index != -1) {
                        selection = selection.toMutableList().apply { this[index] = checkbox.next() }
                    }
                }
                SettingsRow(
                    title = checkbox.value.visualName,
                    leading = {
                        when (checkbox) {
                            is CheckboxState.TriState -> TriStateCheckbox(
                                state = checkbox.asToggleableState(),
                                onClick = null,
                            )
                            is CheckboxState.State -> Checkbox(checked = checkbox.isChecked, onCheckedChange = null)
                        }
                    },
                    role = Role.Checkbox,
                    onClick = onChange,
                )
            }
        }
    }
}
