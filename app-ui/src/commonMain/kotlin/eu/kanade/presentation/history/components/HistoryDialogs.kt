package eu.kanade.presentation.history.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.PreviewLightDark
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Delete
import mihon.icons.materialsymbols.rounded.DeleteSweep
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.dialog.ConfirmDialog
import mihon.presentation.kit.dialog.Dialog
import mihon.presentation.kit.dialog.DialogAction
import mihon.presentation.kit.list.CheckRow
import mihon.presentation.kit.theme.KatariPreviewTheme
import mihon.presentation.kit.theme.KatariTheme
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun HistoryDeleteDialog(
    onDismissRequest: () -> Unit,
    onDelete: (Boolean) -> Unit,
) {
    var removeEverything by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismissRequest,
        title = stringResource(MR.strings.action_remove),
        icon = MaterialSymbols.Rounded.Delete,
        confirm = DialogAction(
            label = stringResource(MR.strings.action_remove),
            onClick = {
                onDelete(removeEverything)
                onDismissRequest()
            },
            destructive = true,
        ),
        dismiss = DialogAction(stringResource(MR.strings.action_cancel), onDismissRequest),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s)) {
            Text(text = stringResource(MR.strings.dialog_with_checkbox_remove_description))
            CheckRow(
                title = stringResource(MR.strings.dialog_with_checkbox_reset),
                checked = removeEverything,
                onCheckedChange = { removeEverything = it },
            )
        }
    }
}

@Composable
fun HistoryDeleteAllDialog(
    onDismissRequest: () -> Unit,
    onDelete: () -> Unit,
) {
    ConfirmDialog(
        title = stringResource(MR.strings.action_remove_everything),
        text = stringResource(MR.strings.clear_history_confirmation),
        confirmLabel = stringResource(MR.strings.action_remove),
        onConfirm = {
            onDelete()
            onDismissRequest()
        },
        onDismissRequest = onDismissRequest,
        destructive = true,
        icon = MaterialSymbols.Rounded.DeleteSweep,
    )
}

@PreviewLightDark
@Composable
private fun HistoryDeleteDialogPreview() {
    KatariPreviewTheme {
        HistoryDeleteDialog(
            onDismissRequest = {},
            onDelete = {},
        )
    }
}
