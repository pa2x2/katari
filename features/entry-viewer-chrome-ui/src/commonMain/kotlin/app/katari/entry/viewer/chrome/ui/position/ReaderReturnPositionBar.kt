package app.katari.entry.viewer.chrome.ui.position

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.rounded.Close
import app.katari.presentation.kit.button.Button
import app.katari.presentation.kit.button.ButtonStyle
import app.katari.presentation.kit.button.IconButton
import app.katari.presentation.kit.surface.OverlayPanel
import app.katari.presentation.kit.surface.OverlayStyle
import app.katari.presentation.kit.theme.KatariTheme
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** Shared action for returning to the position before an explicit reader jump. */
@Composable
fun ReaderReturnPositionBar(
    onReturn: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OverlayPanel(
        modifier = modifier.fillMaxWidth().padding(horizontal = KatariTheme.spacing.l),
        style = OverlayStyle.Solid,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(
                label = stringResource(MR.strings.reader_navigation_return),
                onClick = onReturn,
                modifier = Modifier.weight(1f),
                style = ButtonStyle.Text,
            )
            IconButton(
                icon = MaterialSymbols.Rounded.Close,
                contentDescription = stringResource(MR.strings.reader_navigation_dismiss_return),
                onClick = onDismiss,
            )
        }
    }
}
