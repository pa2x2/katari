package eu.kanade.presentation.browse.components

import androidx.compose.runtime.Composable
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.dialog.Dialog
import mihon.presentation.kit.dialog.DialogAction
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** Why a store's torrent extension is listed but can't be installed. */
@Composable
internal fun ExtensionTorrentListingDialog(
    extensionName: String,
    onDismissRequest: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        title = stringResource(MR.strings.ext_torrent_dialog),
        confirm = DialogAction(stringResource(MR.strings.action_ok), onDismissRequest),
    ) {
        Text(text = stringResource(MR.strings.ext_torrent_listing_message, extensionName))
    }
}
