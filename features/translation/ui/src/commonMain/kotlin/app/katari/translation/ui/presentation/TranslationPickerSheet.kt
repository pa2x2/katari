package app.katari.translation.ui.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import app.katari.presentation.kit.sheet.Sheet
import app.katari.presentation.kit.sheet.SheetHeader

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
