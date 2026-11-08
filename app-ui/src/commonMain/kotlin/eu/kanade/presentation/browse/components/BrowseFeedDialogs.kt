package eu.kanade.presentation.browse.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.rounded.Delete
import app.katari.presentation.kit.dialog.ConfirmDialog
import app.katari.presentation.kit.dialog.Dialog
import app.katari.presentation.kit.dialog.DialogAction
import app.katari.presentation.kit.field.TextField
import app.katari.presentation.kit.list.ToggleRow
import app.katari.presentation.kit.theme.KatariTheme
import dev.icerock.moko.resources.StringResource
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun BrowseFeedNameDialog(
    title: StringResource,
    initialValue: String = "",
    initialChronological: Boolean = true,
    duplicateName: (String) -> Boolean,
    onDismissRequest: () -> Unit,
    onConfirm: (String, Boolean) -> Unit,
) {
    var value by remember(initialValue) { mutableStateOf(initialValue) }
    var chronological by remember(initialChronological) { mutableStateOf(initialChronological) }
    val trimmedValue = value.trim()
    val isDuplicate = trimmedValue.isNotEmpty() && duplicateName(trimmedValue)

    Dialog(
        onDismissRequest = onDismissRequest,
        title = stringResource(title),
        confirm = DialogAction(
            label = stringResource(MR.strings.action_save),
            onClick = { onConfirm(trimmedValue, chronological) },
            enabled = trimmedValue.isNotEmpty() && !isDuplicate,
        ),
        dismiss = DialogAction(stringResource(MR.strings.action_cancel), onDismissRequest),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.m)) {
            TextField(
                value = value,
                onValueChange = { value = it },
                modifier = Modifier.fillMaxWidth(),
                label = stringResource(MR.strings.name),
                supportingText = if (isDuplicate) stringResource(MR.strings.browse_feed_preset_exists) else null,
                isError = isDuplicate,
            )

            ToggleRow(
                title = stringResource(MR.strings.browse_feed_chronological),
                subtitle = stringResource(MR.strings.browse_feed_chronological_summary),
                checked = chronological,
                onCheckedChange = { chronological = it },
            )
        }
    }
}

@Composable
fun DeleteBrowsePresetDialog(
    presetName: String,
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
) {
    ConfirmDialog(
        title = stringResource(MR.strings.are_you_sure),
        text = stringResource(MR.strings.browse_delete_preset_confirmation, presetName),
        confirmLabel = stringResource(MR.strings.action_delete),
        onConfirm = {
            onDismissRequest()
            onConfirm()
        },
        onDismissRequest = onDismissRequest,
        destructive = true,
        icon = MaterialSymbols.Rounded.Delete,
    )
}
