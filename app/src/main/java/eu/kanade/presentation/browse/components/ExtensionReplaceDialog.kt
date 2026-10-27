package eu.kanade.presentation.browse.components

import androidx.compose.runtime.Composable
import eu.kanade.tachiyomi.ui.browse.extension.ExtensionReplacement
import mihon.presentation.kit.dialog.ConfirmDialog
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun ExtensionReplaceDialog(
    replacement: ExtensionReplacement,
    onConfirm: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    val (listing, installed) = replacement
    val installedStore = installed.store
    ConfirmDialog(
        title = stringResource(MR.strings.ext_replace_title, installed.name),
        text = if (installedStore != null) {
            stringResource(
                MR.strings.ext_replace_message,
                installed.name,
                installedStore.name,
                installed.versionName,
                listing.store.name,
                listing.versionName,
            )
        } else {
            stringResource(
                MR.strings.ext_replace_message_no_store,
                installed.name,
                installed.versionName,
                listing.store.name,
                listing.versionName,
            )
        },
        confirmLabel = stringResource(MR.strings.ext_replace),
        onConfirm = onConfirm,
        onDismissRequest = onDismissRequest,
    )
}
