package eu.kanade.presentation.browse.components

import androidx.compose.runtime.Composable
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.dialog.Dialog
import mihon.presentation.kit.dialog.DialogAction
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun ExtensionTrustDialog(
    onClickConfirm: () -> Unit,
    onClickDismiss: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        title = stringResource(MR.strings.untrusted_extension),
        confirm = DialogAction(stringResource(MR.strings.ext_trust), onClickConfirm),
        dismiss = DialogAction(stringResource(MR.strings.ext_uninstall), onClickDismiss),
    ) {
        Text(text = stringResource(MR.strings.untrusted_extension_message))
    }
}
