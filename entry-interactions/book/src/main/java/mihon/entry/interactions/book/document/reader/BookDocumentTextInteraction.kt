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

internal sealed interface BookDocumentTextSelection {
    val ownerIdentity: String

    data class Changed(
        override val ownerIdentity: String,
        val identity: String,
        val text: String,
        val boundsInReaderRoot: RectF,
    ) : BookDocumentTextSelection

    data class Cleared(
        override val ownerIdentity: String,
    ) : BookDocumentTextSelection
}

internal val LocalBookDocumentTextInteraction = compositionLocalOf { BookDocumentTextInteraction.Disabled }
internal val LocalBookDocumentSectionKey = compositionLocalOf<String?> { null }
