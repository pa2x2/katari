package mihon.entry.interactions.book.document.reader

import androidx.compose.foundation.text.contextmenu.provider.TextContextMenuDataProvider
import androidx.compose.foundation.text.contextmenu.provider.TextContextMenuProvider
import kotlinx.coroutines.awaitCancellation

/** Observes Compose's selection completion signal, including handles in separate popup windows. */
internal class BookDocumentSelectionSettlementToolbar(
    private val selection: BookDocumentChapterSelection,
    private val platformToolbar: TextContextMenuProvider?,
) : TextContextMenuProvider {
    override suspend fun showTextContextMenu(dataProvider: TextContextMenuDataProvider) {
        // Compose requests the toolbar after release and cancels it when selection resumes.
        selection.setSelectionSettled(true)
        try {
            if (selection.interaction.showTextSelectionMenu) {
                platformToolbar?.showTextContextMenu(dataProvider)
            }
            // Closing or disabling the menu does not start another selection gesture. Keep
            // observing until Compose cancels this request on drag, clear, or disposal.
            awaitCancellation()
        } finally {
            selection.setSelectionSettled(false)
        }
    }
}
