package eu.kanade.presentation.entry.components

import androidx.compose.runtime.Composable
import eu.kanade.presentation.entry.entryTypePresentation
import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Delete
import mihon.presentation.kit.dialog.ConfirmDialog
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun DeleteChaptersDialog(
    entryType: EntryType,
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
) {
    ConfirmDialog(
        title = stringResource(MR.strings.are_you_sure),
        text = stringResource(entryType.entryTypePresentation().deleteChildrenConfirmationLabel),
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
