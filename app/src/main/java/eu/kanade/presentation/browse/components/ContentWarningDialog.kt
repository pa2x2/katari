package eu.kanade.presentation.browse.components

import androidx.compose.runtime.Composable
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.dialog.Dialog
import mihon.presentation.kit.dialog.DialogAction
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun ContentWarningDialog(
    label: ContentWarningLabel,
    onDismissRequest: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        title = stringResource(label.title),
        confirm = DialogAction(stringResource(MR.strings.action_ok), onDismissRequest),
    ) {
        Text(text = stringResource(label.description))
    }
}
