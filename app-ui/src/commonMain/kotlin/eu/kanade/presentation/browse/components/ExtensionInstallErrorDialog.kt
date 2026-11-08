package eu.kanade.presentation.browse.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import app.katari.presentation.kit.dialog.Dialog
import app.katari.presentation.kit.dialog.DialogAction
import app.katari.presentation.kit.theme.KatariTheme
import eu.kanade.tachiyomi.extension.model.InstallStep
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun ExtensionInstallErrorDialog(
    error: InstallStep.Error,
    onDismissRequest: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        title = stringResource(MR.strings.ext_install_error),
        confirm = DialogAction(stringResource(MR.strings.action_ok), onDismissRequest),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s)) {
            ExtensionFailureDetails(message = error.message, stackTrace = error.stackTrace)
        }
    }
}
