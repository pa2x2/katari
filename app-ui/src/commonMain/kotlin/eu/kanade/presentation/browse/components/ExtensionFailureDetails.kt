package eu.kanade.presentation.browse.components

import androidx.compose.runtime.Composable
import app.katari.presentation.kit.button.Button
import app.katari.presentation.kit.button.ButtonStyle
import app.katari.presentation.kit.content.Text
import app.katari.presentation.kit.theme.ContentTone
import app.katari.presentation.kit.theme.TextRole
import app.katari.ui.di.LocalAppUiGraph
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
