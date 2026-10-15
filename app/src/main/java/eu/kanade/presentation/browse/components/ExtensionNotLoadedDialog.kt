package eu.kanade.presentation.browse.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import dev.icerock.moko.resources.StringResource
import eu.kanade.tachiyomi.extension.model.Extension
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.dialog.Dialog
import mihon.presentation.kit.dialog.DialogAction
import mihon.presentation.kit.theme.KatariTheme
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun ExtensionNotLoadedDialog(
    reason: Extension.NotLoaded.Reason,
    onClickUninstall: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        title = stringResource(MR.strings.ext_not_loaded_dialog),
        confirm = DialogAction(stringResource(MR.strings.action_ok), onDismissRequest),
        dismiss = DialogAction(stringResource(MR.strings.ext_uninstall), onClickUninstall),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s)) {
            Text(text = stringResource(reason.descriptionRes))

            if (reason is Extension.NotLoaded.Reason.Failed) {
                ExtensionFailureDetails(message = reason.message, stackTrace = reason.stackTrace)
            }
        }
    }
}

private val Extension.NotLoaded.Reason.descriptionRes: StringResource
    get() = when (this) {
        is Extension.NotLoaded.Reason.Untrusted -> MR.strings.untrusted_extension_message
        Extension.NotLoaded.Reason.Filtered -> MR.strings.ext_filtered_message
        Extension.NotLoaded.Reason.Unsigned -> MR.strings.ext_unsigned_message
        Extension.NotLoaded.Reason.UnsupportedLibVersion -> MR.strings.ext_unsupported_message
        Extension.NotLoaded.Reason.Malformed -> MR.strings.ext_malformed_message
        is Extension.NotLoaded.Reason.Failed -> MR.strings.ext_load_failed_message
    }
