package eu.kanade.presentation.browse.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import eu.kanade.tachiyomi.extension.kind.unsupportedSdk
import eu.kanade.tachiyomi.extension.model.Extension
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.dialog.Dialog
import mihon.presentation.kit.dialog.DialogAction
import mihon.presentation.kit.theme.KatariTheme
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun ExtensionNotLoadedDialog(
    extensionName: String,
    reason: Extension.NotLoaded.Reason,
    onClickUninstall: () -> Unit,
    onShowCompatibility: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        title = stringResource(
            if (reason == Extension.NotLoaded.Reason.Torrent) {
                MR.strings.ext_torrent_dialog
            } else {
                MR.strings.ext_not_loaded_dialog
            },
        ),
        confirm = DialogAction(stringResource(MR.strings.action_ok), onDismissRequest),
        dismiss = DialogAction(stringResource(MR.strings.ext_uninstall), onClickUninstall),
        neutral = DialogAction(stringResource(MR.strings.ext_compatibility_action), onShowCompatibility)
            .takeIf { reason is Extension.NotLoaded.Reason.UnsupportedLibVersion },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s)) {
            Text(text = reason.description(extensionName))

            when (reason) {
                is Extension.NotLoaded.Reason.Failed ->
                    ExtensionFailureDetails(message = reason.message, stackTrace = reason.stackTrace)
                is Extension.NotLoaded.Reason.SourceConflicts ->
                    sourceConflictLines(reason.conflicts).forEach { Text(text = it) }
                is Extension.NotLoaded.Reason.UnsupportedLibVersion ->
                    unsupportedSdkAdvice(reason.unsupportedSdk(), installed = true)?.let { Text(text = it) }
                else -> Unit
            }
        }
    }
}

@Composable
private fun Extension.NotLoaded.Reason.description(extensionName: String): String = when (this) {
    is Extension.NotLoaded.Reason.Untrusted -> stringResource(MR.strings.untrusted_extension_message)
    Extension.NotLoaded.Reason.Filtered -> stringResource(MR.strings.ext_filtered_message)
    Extension.NotLoaded.Reason.Unsigned -> stringResource(MR.strings.ext_unsigned_message)
    is Extension.NotLoaded.Reason.UnsupportedLibVersion -> unsupportedSdkMessage(extensionName, unsupportedSdk())
    Extension.NotLoaded.Reason.Torrent -> stringResource(MR.strings.ext_torrent_message, extensionName)
    Extension.NotLoaded.Reason.Malformed -> stringResource(MR.strings.ext_malformed_message)
    Extension.NotLoaded.Reason.Unreadable -> stringResource(MR.strings.ext_unreadable_message)
    Extension.NotLoaded.Reason.Damaged -> stringResource(MR.strings.ext_damaged_message)
    is Extension.NotLoaded.Reason.Failed -> stringResource(MR.strings.ext_load_failed_message)
    is Extension.NotLoaded.Reason.SourceConflicts -> stringResource(MR.strings.ext_source_conflict_message)
}
