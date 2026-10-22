package mihon.presentation.kit.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import mihon.presentation.kit.button.Button
import mihon.presentation.kit.button.ButtonStyle
import mihon.presentation.kit.theme.KatariTheme

/**
 * The buttons closing a dialog or sheet. [confirm] is the one filled button (red when destructive) and [dismiss] is
 * tonal; as a pair they share the width, and a lone button takes all of it. [neutral] is a text button at the start,
 * apart from the others, which then keep their own width at the end.
 */
@Composable
fun DialogActions(
    confirm: DialogAction?,
    modifier: Modifier = Modifier,
    dismiss: DialogAction? = null,
    neutral: DialogAction? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val fill = if (neutral == null) Modifier.weight(1f) else Modifier
        if (neutral != null) {
            ActionButton(neutral, ButtonStyle.Text)
            Spacer(Modifier.weight(1f))
        }
        dismiss?.let { ActionButton(it, ButtonStyle.Tonal, fill) }
        confirm?.let { ActionButton(it, ButtonStyle.Filled, fill) }
    }
}

@Composable
private fun ActionButton(action: DialogAction, style: ButtonStyle, modifier: Modifier = Modifier) {
    Button(
        label = action.label,
        onClick = action.onClick,
        modifier = modifier,
        style = style,
        enabled = action.enabled,
        destructive = action.destructive,
    )
}
