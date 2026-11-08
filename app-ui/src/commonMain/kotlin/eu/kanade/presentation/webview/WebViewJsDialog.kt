package eu.kanade.presentation.webview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import app.katari.presentation.kit.content.Text
import app.katari.presentation.kit.dialog.Dialog
import app.katari.presentation.kit.dialog.DialogAction
import app.katari.presentation.kit.field.TextField
import app.katari.presentation.kit.theme.KatariTheme
import eu.kanade.tachiyomi.ui.webview.browser.BrowserJsDialog
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** Shows [dialog] and answers it once the user does, closing it being Cancel; [onAnswered] follows the answer. */
@Composable
internal fun WebViewJsDialog(dialog: BrowserJsDialog, onAnswered: () -> Unit) {
    val title = dialog.url?.let { url ->
        pageOrigin(url)?.let { stringResource(MR.strings.webview_js_dialog_title, it) }
    }
    fun answer(reply: () -> Unit) {
        reply()
        onAnswered()
    }
    val ok = stringResource(MR.strings.action_ok)
    val cancel = DialogAction(stringResource(MR.strings.action_cancel), { answer(dialog::cancel) })
    when (dialog) {
        is BrowserJsDialog.Alert -> Dialog(
            onDismissRequest = { answer(dialog::cancel) },
            title = title,
            confirm = DialogAction(ok, { answer(dialog::confirm) }),
            content = { Text(dialog.message) },
        )
        is BrowserJsDialog.Confirm -> Dialog(
            onDismissRequest = { answer(dialog::cancel) },
            title = title,
            confirm = DialogAction(ok, { answer(dialog::confirm) }),
            dismiss = cancel,
            content = { Text(dialog.message) },
        )
        is BrowserJsDialog.Prompt -> {
            var value by remember(dialog) {
                mutableStateOf(TextFieldValue(dialog.defaultValue, TextRange(dialog.defaultValue.length)))
            }
            val focusRequester = remember { FocusRequester() }
            LaunchedEffect(focusRequester) { focusRequester.requestFocus() }
            Dialog(
                onDismissRequest = { answer(dialog::cancel) },
                title = title,
                confirm = DialogAction(ok, { answer { dialog.confirm(value.text) } }),
                dismiss = cancel,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.m)) {
                    if (dialog.message.isNotEmpty()) Text(dialog.message)
                    TextField(
                        value = value,
                        onValueChange = { value = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                    )
                }
            }
        }
    }
}

// As Android's web view titles these dialogs: a data: page has no origin to name, and another address that isn't a web
// one is named as it is
private fun pageOrigin(url: String): String? {
    if (url.startsWith("data:", ignoreCase = true)) return null
    return url.toHttpUrlOrNull()?.let { "${it.scheme}://${it.host}" } ?: url
}
