package eu.kanade.presentation.browse.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.util.system.copyToClipboard
import tachiyomi.core.common.i18n.stringResource
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
        style = MaterialTheme.typography.bodySmall,
        fontFamily = FontFamily.Monospace,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    if (stackTrace != null) {
        val context = LocalContext.current
        TextButton(
            onClick = {
                context.copyToClipboard(
                    label = context.stringResource(MR.strings.ext_copy_stacktrace),
                    content = stackTrace,
                )
            },
            contentPadding = PaddingValues(0.dp),
        ) {
            Text(text = stringResource(MR.strings.ext_copy_stacktrace))
        }
    }
}
