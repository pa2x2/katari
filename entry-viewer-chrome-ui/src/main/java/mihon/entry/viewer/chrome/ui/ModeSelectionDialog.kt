package mihon.entry.viewer.chrome.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Check
import mihon.presentation.kit.button.Button
import mihon.presentation.kit.button.ButtonStyle
import mihon.presentation.kit.theme.KatariTheme
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun ModeSelectionDialog(
    onApply: () -> Unit,
    onUseDefault: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Column(modifier = Modifier.padding(vertical = KatariTheme.spacing.l)) {
        content()

        Row(modifier = Modifier.padding(horizontal = KatariTheme.spacing.rowInset)) {
            onUseDefault?.let {
                Button(
                    label = stringResource(MR.strings.action_revert_to_default),
                    onClick = it,
                    style = ButtonStyle.Outlined,
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                label = stringResource(MR.strings.action_apply),
                onClick = onApply,
                style = ButtonStyle.Tonal,
                icon = MaterialSymbols.Rounded.Check,
            )
        }
    }
}
