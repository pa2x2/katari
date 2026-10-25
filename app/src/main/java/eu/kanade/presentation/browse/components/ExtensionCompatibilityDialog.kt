package eu.kanade.presentation.browse.components

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import eu.kanade.tachiyomi.extension.kind.ExtensionFamily
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.dialog.Dialog
import mihon.presentation.kit.dialog.DialogAction
import mihon.presentation.kit.list.InfoRow
import mihon.presentation.kit.list.SettingsRow
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** The extension libraries this build loads and the versions of each, which is what decides what it can install. */
@Composable
internal fun ExtensionCompatibilityDialog(onDismissRequest: () -> Unit) {
    Dialog(
        onDismissRequest = onDismissRequest,
        title = stringResource(MR.strings.ext_compatibility),
        confirm = DialogAction(stringResource(MR.strings.action_ok), onDismissRequest),
    ) {
        Column {
            Text(text = stringResource(MR.strings.ext_compatibility_summary))
            ExtensionFamily.entries.forEach { family ->
                SettingsRow(
                    title = stringResource(family.titleRes),
                    subtitle = "${family.library} ${family.versionsText}",
                )
            }
            InfoRow(text = stringResource(MR.strings.ext_compatibility_other), showIcon = false)
        }
    }
}
