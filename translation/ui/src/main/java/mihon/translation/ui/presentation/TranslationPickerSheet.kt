package mihon.translation.ui.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import mihon.presentation.kit.sheet.Sheet
import mihon.presentation.kit.sheet.SheetHeader

/** A titled sheet for one of the translation pickers, such as a language or engine list. */
@Composable
fun TranslationPickerSheet(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Sheet(onDismissRequest = onDismiss, capHeight = true) {
        Column {
            SheetHeader(title)
            content()
        }
    }
}
