package mihon.entry.interactions.book.document.reader

import android.graphics.RectF
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.geometry.Offset

internal data class BookDocumentTextInteraction(
    val observeSelections: Boolean,
    val rootPositionInWindow: Offset,
    val onSelection: (BookDocumentTextSelection) -> Unit,
    val isReaderTapBlocked: () -> Boolean,
    val onBlockedReaderTap: () -> Unit,
    val onNonLinkTap: (x: Float, width: Float) -> Unit,
    val showTextSelectionMenu: Boolean = true,
    val selectionActions: Set<BookDocumentSelectionAction> = emptySet(),
    val activeSpeechSelectionIdentity: String? = null,
    val onSelectionAction: (
        ownerIdentity: String,
        selectionIdentity: String,
        action: BookDocumentSelectionAction,
    ) -> Unit = { _, _, _ -> },
) {
    companion object {
        val Disabled = BookDocumentTextInteraction(
            observeSelections = false,
            rootPositionInWindow = Offset.Zero,
            onSelection = {},
            isReaderTapBlocked = { false },
            onBlockedReaderTap = {},
            onNonLinkTap = { _, _ -> },
        )
    }
}

internal enum class BookDocumentSelectionAction {
    Listen,
    Translate,
}

internal sealed interface BookDocumentTextSelection {
    val ownerIdentity: String

    data class Changed(
        override val ownerIdentity: String,
        val identity: String,
        val text: String,
        val languageContextText: String,
        val boundsInReaderRoot: RectF,
        val isSettled: Boolean,
    ) : BookDocumentTextSelection

    data class Cleared(
        override val ownerIdentity: String,
    ) : BookDocumentTextSelection
}

internal val LocalBookDocumentTextInteraction = compositionLocalOf { BookDocumentTextInteraction.Disabled }
internal val LocalBookDocumentSectionKey = compositionLocalOf<String?> { null }
