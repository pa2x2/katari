package eu.kanade.presentation.browse.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import eu.kanade.tachiyomi.extension.kind.UnsupportedSdk
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.dialog.Dialog
import mihon.presentation.kit.dialog.DialogAction
import mihon.presentation.kit.theme.KatariTheme
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Why a store's extension is listed but can't be installed: it was built with a library version this app doesn't run.
 * [sdk] is `null` when its version can't be read, so only the general rule can be given.
 */
@Composable
internal fun ExtensionUnsupportedSdkListingDialog(
    extensionName: String,
    sdk: UnsupportedSdk?,
    onShowCompatibility: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        title = stringResource(MR.strings.ext_unsupported_sdk),
        confirm = DialogAction(stringResource(MR.strings.action_ok), onDismissRequest),
        neutral = DialogAction(stringResource(MR.strings.ext_compatibility_action), onShowCompatibility),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s)) {
            if (sdk == null) {
                Text(text = stringResource(MR.strings.ext_compatibility_other))
            } else {
                Text(text = unsupportedSdkMessage(extensionName, sdk))
                unsupportedSdkAdvice(sdk, installed = false)?.let { Text(text = it) }
            }
        }
    }
}
