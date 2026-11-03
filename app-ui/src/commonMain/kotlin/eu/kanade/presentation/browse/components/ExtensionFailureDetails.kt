package eu.kanade.presentation.browse.components

import androidx.compose.runtime.Composable
import mihon.app.ui.di.LocalAppUiGraph
import mihon.presentation.kit.button.Button
import mihon.presentation.kit.button.ButtonStyle
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.TextRole
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * What went wrong with an extension in the words of whatever failed, which are usually not translated, so they're
 * set apart from the dialog's own text.
 */
@Composable
internal fun ExtensionFailureDetails(
    message: String,
    stackTrace: String?,
) {
    Text(
        text = message,
        role = TextRole.Code,
        tone = ContentTone.Secondary,
    )

    if (stackTrace != null) {
        val textClipboard = LocalAppUiGraph.current.textClipboard
        val label = stringResource(MR.strings.ext_copy_stacktrace)
        Button(
            label = label,
            onClick = { textClipboard.copy(label = label, text = stackTrace) },
            style = ButtonStyle.Text,
        )
    }
}
