package app.katari.desktop.extension.dialog

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import app.katari.presentation.kit.dialog.DialogAction
import app.katari.presentation.kit.dialog.WebPageDialog
import app.katari.webview.jcef.surface.ChromiumExtensionPage
import tachiyomi.i18n.MR
import tachiyomi.i18n.action_cancel
import tachiyomi.presentation.core.i18n.stringResource

/** The extensions' [dialogs] over the window's screens, each the app's dialog around its extension's page. */
@Composable
fun ExtensionDialogsHost(dialogs: WindowExtensionDialogs) {
    val shown by dialogs.dialogs.collectAsState()
    shown.forEach { dialog ->
        key(dialog.key) {
            WebPageDialog(
                onDismissRequest = dialog.requestClose,
                title = dialog.title,
                dismiss = DialogAction(stringResource(MR.strings.action_cancel), dialog.requestClose)
                    .takeIf { dialog.closable },
                dismissOnClickOutside = dialog.closesOnClickOutside,
            ) { modifier -> ChromiumExtensionPage(dialog.page, modifier) }
        }
    }
}
