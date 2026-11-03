package eu.kanade.presentation.updates

import androidx.compose.runtime.Composable
import eu.kanade.presentation.entry.selectionEntryTypePresentation
import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Delete
import mihon.presentation.kit.dialog.ConfirmDialog
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun UpdatesDeleteConfirmationDialog(
    entryTypes: Iterable<EntryType>,
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
) {
    val presentation = entryTypes.selectionEntryTypePresentation()

    ConfirmDialog(
        title = stringResource(MR.strings.are_you_sure),
        text = stringResource(presentation.deleteChildrenConfirmationLabel),
        confirmLabel = stringResource(MR.strings.action_delete),
        onConfirm = {
            onConfirm()
            onDismissRequest()
        },
        onDismissRequest = onDismissRequest,
        destructive = true,
        icon = MaterialSymbols.Rounded.Delete,
    )
}
