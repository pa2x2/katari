package mihon.translation.ui.presentation.content

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import mihon.presentation.kit.button.Button
import mihon.presentation.kit.button.ButtonStyle
import mihon.translation.ui.presentation.TranslationSessionExternalAction
import mihon.translation.ui.presentation.language.TranslationDirectionText
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/*
 * The parts a session state that waits on the reader is built from: a message, its main action and a link to the
 * engine's documentation. The compact popup shortens the message and leaves the documentation to its header.
 */

@Composable
internal fun SessionMessage(
    text: String,
    compact: Boolean,
) {
    TranslationDirectionText(
        text = text,
        maxLines = if (compact) COMPACT_MESSAGE_MAX_LINES else Int.MAX_VALUE,
        overflow = if (compact) TextOverflow.Ellipsis else TextOverflow.Clip,
    )
}

@Composable
internal fun SessionActionButton(
    label: String,
    onClick: () -> Unit,
) {
    Button(label = label, onClick = onClick, modifier = Modifier.fillMaxWidth())
}

@Composable
internal fun DocumentationAction(
    url: String?,
    onExternalAction: (TranslationSessionExternalAction) -> Unit,
    compact: Boolean = false,
) {
    if (url == null || compact) return
    Button(
        label = stringResource(MR.strings.action_learn_more),
        onClick = { onExternalAction(TranslationSessionExternalAction.OpenDocumentation(url)) },
        style = ButtonStyle.Text,
    )
}

private const val COMPACT_MESSAGE_MAX_LINES = 3
