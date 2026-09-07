package mihon.entry.interactions.book.document.reader

import androidx.compose.foundation.text.contextmenu.provider.LocalTextContextMenuToolbarProvider
import androidx.compose.foundation.text.contextmenu.provider.TextContextMenuDataProvider
import androidx.compose.foundation.text.contextmenu.provider.TextContextMenuProvider
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import kotlinx.coroutines.awaitCancellation

@Composable
internal fun ProvideBookDocumentSelectionSettlementToolbar(
    selection: BookDocumentChapterSelection,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    // Compose creates its default platform provider inside SelectionContainer. Establish it
    // before decorating it; installing our wrapper first would suppress that initialization.
    // The content's SelectionContainer owns the selectable text, so this outer registrar is empty.
    SelectionContainer(modifier = modifier) {
        val platformToolbar = checkNotNull(LocalTextContextMenuToolbarProvider.current)
        val toolbar = remember(selection, platformToolbar) {
            BookDocumentSelectionSettlementToolbar(selection, platformToolbar)
        }
        CompositionLocalProvider(LocalTextContextMenuToolbarProvider provides toolbar, content = content)
    }
}

/** Observes Compose's selection completion signal, including handles in separate popup windows. */
private class BookDocumentSelectionSettlementToolbar(
    private val selection: BookDocumentChapterSelection,
    private val platformToolbar: TextContextMenuProvider,
) : TextContextMenuProvider {
    override suspend fun showTextContextMenu(dataProvider: TextContextMenuDataProvider) {
        // Compose requests the toolbar after release and cancels it when selection resumes.
        selection.setSelectionSettled(true)
        try {
            if (selection.interaction.showTextSelectionMenu) {
                platformToolbar.showTextContextMenu(dataProvider)
            }
            // Closing or disabling the menu does not start another selection gesture. Keep
            // observing until Compose cancels this request on drag, clear, or disposal.
            awaitCancellation()
        } finally {
            selection.setSelectionSettled(false)
        }
    }
}
