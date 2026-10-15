package mihon.presentation.kit.sheet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import mihon.presentation.kit.button.Button
import mihon.presentation.kit.button.ButtonStyle
import mihon.presentation.kit.dialog.DialogAction
import mihon.presentation.kit.theme.KatariTheme

/**
 * The buttons closing a sheet that asks for a decision, such as picking a value: [neutral] at the start, [dismiss] and
 * [confirm] at the end. Confirm is the sheet's main action, so it is filled (red when destructive); the others are text.
 */
@Composable
fun SheetActions(
    confirm: DialogAction,
    modifier: Modifier = Modifier,
    dismiss: DialogAction? = null,
    neutral: DialogAction? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = KatariTheme.spacing.rowInset, vertical = KatariTheme.spacing.m),
        horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s, Alignment.End),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        neutral?.let {
            Button(label = it.label, onClick = it.onClick, style = ButtonStyle.Text, enabled = it.enabled)
            Spacer(modifier = Modifier.weight(1f))
        }
        dismiss?.let {
            Button(label = it.label, onClick = it.onClick, style = ButtonStyle.Text, enabled = it.enabled)
        }
        Button(
            label = confirm.label,
            onClick = confirm.onClick,
            enabled = confirm.enabled,
            destructive = confirm.destructive,
        )
    }
}
