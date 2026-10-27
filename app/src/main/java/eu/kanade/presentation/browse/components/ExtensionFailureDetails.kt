package eu.kanade.presentation.browse.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import eu.kanade.tachiyomi.util.system.copyToClipboard
import mihon.presentation.kit.button.Button
import mihon.presentation.kit.button.ButtonStyle
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.TextRole
import tachiyomi.i18n.*
import tachiyomi.i18n.context.stringResource
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
        val context = LocalContext.current
        Button(
            label = stringResource(MR.strings.ext_copy_stacktrace),
            onClick = {
                context.copyToClipboard(
                    label = context.stringResource(MR.strings.ext_copy_stacktrace),
                    content = stackTrace,
                )
            },
            style = ButtonStyle.Text,
        )
    }
}
