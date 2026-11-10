package eu.kanade.presentation.webview.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.katari.presentation.kit.content.Text
import app.katari.presentation.kit.dialog.Dialog
import app.katari.presentation.kit.dialog.DialogAction
import app.katari.presentation.kit.list.InfoRow
import app.katari.presentation.kit.theme.KatariTheme
import app.katari.presentation.kit.theme.TextRole
import eu.kanade.tachiyomi.ui.webview.setup.WebViewSetup
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.shortFileSize
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Asks before the web view's Chromium is downloaded, saying what the download costs: its size, the wait, and that an
 * update of the app can need another. [onDownload] follows the user agreeing, [onDismiss] putting it off.
 */
@Composable
fun WebViewSetupDialog(setup: WebViewSetup, onDownload: () -> Unit, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        title = stringResource(MR.strings.webview_setup_title),
        confirm = DialogAction(stringResource(MR.strings.action_download), onDownload),
        dismiss = DialogAction(stringResource(MR.strings.action_not_now), onDismiss),
    ) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s),
        ) {
            Text(stringResource(MR.strings.webview_setup_message), role = TextRole.Body)
            InfoRow(
                text = stringResource(
                    MR.strings.webview_setup_size,
                    shortFileSize(setup.downloadBytes),
                    shortFileSize(setup.installedBytes),
                ),
            )
            InfoRow(stringResource(MR.strings.webview_setup_wait))
            InfoRow(stringResource(MR.strings.webview_setup_updates))
        }
    }
}
