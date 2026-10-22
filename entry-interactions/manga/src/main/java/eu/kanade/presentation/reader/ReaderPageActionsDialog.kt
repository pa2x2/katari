package eu.kanade.presentation.reader

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.ContentCopy
import mihon.icons.materialsymbols.rounded.Photo
import mihon.icons.materialsymbols.rounded.Save
import mihon.icons.materialsymbols.rounded.Share
import mihon.presentation.kit.button.StackedActionButton
import mihon.presentation.kit.dialog.ConfirmDialog
import mihon.presentation.kit.sheet.Sheet
import mihon.presentation.kit.theme.KatariTheme
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun ReaderPageActionsDialog(
    onDismissRequest: () -> Unit,
    onSetAsCover: () -> Unit,
    onShare: (Boolean) -> Unit,
    onSave: () -> Unit,
) {
    var showSetCoverDialog by remember { mutableStateOf(false) }

    Sheet(onDismissRequest = onDismissRequest) {
        Row(
            // Inset like the cards of other sheets.
            modifier = Modifier.padding(horizontal = KatariTheme.spacing.m, vertical = KatariTheme.spacing.l),
            horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s),
        ) {
            StackedActionButton(
                modifier = Modifier.weight(1f),
                label = stringResource(MR.strings.set_as_cover),
                icon = MaterialSymbols.Rounded.Photo,
                onClick = { showSetCoverDialog = true },
            )
            StackedActionButton(
                modifier = Modifier.weight(1f),
                label = stringResource(MR.strings.action_copy_to_clipboard),
                icon = MaterialSymbols.Rounded.ContentCopy,
                onClick = {
                    onShare(true)
                    onDismissRequest()
                },
            )
            StackedActionButton(
                modifier = Modifier.weight(1f),
                label = stringResource(MR.strings.action_share),
                icon = MaterialSymbols.Rounded.Share,
                onClick = {
                    onShare(false)
                    onDismissRequest()
                },
            )
            StackedActionButton(
                modifier = Modifier.weight(1f),
                label = stringResource(MR.strings.action_save),
                icon = MaterialSymbols.Rounded.Save,
                onClick = {
                    onSave()
                    onDismissRequest()
                },
            )
        }
    }

    if (showSetCoverDialog) {
        SetCoverDialog(
            onConfirm = {
                onSetAsCover()
                showSetCoverDialog = false
            },
            onDismiss = { showSetCoverDialog = false },
        )
    }
}

@Composable
private fun SetCoverDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    ConfirmDialog(
        title = stringResource(MR.strings.set_as_cover),
        text = stringResource(MR.strings.confirm_set_image_as_cover),
        confirmLabel = stringResource(MR.strings.set_as_cover),
        onConfirm = onConfirm,
        onDismissRequest = onDismiss,
    )
}
