package mihon.entry.viewer.chrome.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import mihon.entry.viewer.chrome.ui.R
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Close
import mihon.presentation.kit.button.Button
import mihon.presentation.kit.button.ButtonStyle
import mihon.presentation.kit.button.IconButton
import mihon.presentation.kit.surface.Surface
import mihon.presentation.kit.surface.SurfaceContainer
import mihon.presentation.kit.theme.KatariTheme

/** Shared action for returning to the position before an explicit reader jump. */
@Composable
fun ReaderReturnPositionBar(
    onReturn: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth().padding(horizontal = KatariTheme.spacing.l),
        container = SurfaceContainer.Surface,
        shape = KatariTheme.shapes.medium,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(
                label = stringResource(R.string.reader_navigation_return),
                onClick = onReturn,
                modifier = Modifier.weight(1f),
                style = ButtonStyle.Text,
            )
            IconButton(
                icon = MaterialSymbols.Rounded.Close,
                contentDescription = stringResource(R.string.reader_navigation_dismiss_return),
                onClick = onDismiss,
            )
        }
    }
}
