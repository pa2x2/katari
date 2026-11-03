package mihon.presentation.kit.sheet

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import mihon.presentation.kit.dialog.DialogAction
import mihon.presentation.kit.dialog.DialogActions
import mihon.presentation.kit.theme.KatariTheme

/**
 * The buttons closing a sheet that asks for a decision, such as picking a value. They look like a dialog's
 * ([DialogActions]): [neutral] at the start, [dismiss] and [confirm] as a pair at the end.
 */
@Composable
fun SheetActions(
    confirm: DialogAction?,
    modifier: Modifier = Modifier,
    dismiss: DialogAction? = null,
    neutral: DialogAction? = null,
) {
    DialogActions(
        confirm = confirm,
        modifier = modifier.padding(horizontal = KatariTheme.spacing.rowInset, vertical = KatariTheme.spacing.m),
        dismiss = dismiss,
        neutral = neutral,
    )
}
