package eu.kanade.presentation.updates

import androidx.compose.runtime.Composable
import eu.kanade.presentation.entry.selectionEntryTypePresentation
import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.dialog.Dialog
import mihon.presentation.kit.dialog.DialogAction
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun UpdatesDeleteConfirmationDialog(
    entryTypes: Iterable<EntryType>,
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
) {
    val presentation = entryTypes.selectionEntryTypePresentation()

    Dialog(
        onDismissRequest = onDismissRequest,
        title = null,
        confirm = DialogAction(
            label = stringResource(MR.strings.action_ok),
            onClick = {
                onConfirm()
                onDismissRequest()
            },
            destructive = true,
        ),
        dismiss = DialogAction(stringResource(MR.strings.action_cancel), onDismissRequest),
    ) {
        Text(text = stringResource(presentation.deleteChildrenConfirmationLabel))
    }
}
