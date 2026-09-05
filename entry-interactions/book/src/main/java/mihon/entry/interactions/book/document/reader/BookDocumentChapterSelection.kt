package mihon.entry.interactions.book.document.reader

import android.graphics.RectF
import androidx.compose.foundation.text.selection.SelectionState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.IntOffset

internal class BookDocumentChapterSelection(
    ownerIdentity: String,
    private val selectionState: SelectionState,
) {
    private var ownerIdentity = ownerIdentity
    private val selectableLeaves = mutableMapOf<String, BookDocumentSelectableLeaf>()
    private val layouts = mutableMapOf<String, BookDocumentSelectionLayout>()
    private var selectionPresentAtPointerDown = false

    internal var interaction: BookDocumentTextInteraction = BookDocumentTextInteraction.Disabled
    internal var layoutRevision by mutableIntStateOf(0)
        private set
    private var restrictedChapterId by mutableStateOf<Long?>(null)
    private var geometryTokens by mutableStateOf<Set<String>>(emptySet())

    val hasSelection: Boolean
        get() = selectionState.selectedTexts.isNotEmpty()

    fun shouldTrackGeometry(token: String): Boolean =
        hasSelection &&
            interaction.observeSelections &&
            (geometryTokens.isEmpty() || token in geometryTokens)

    fun registerText(leaf: BookDocumentSelectableLeaf) {
        selectableLeaves[leaf.token] = leaf
    }

    fun unregisterText(token: String) {
        selectableLeaves.remove(token)
        layouts.remove(token)
    }

    fun updateTextLayout(token: String, textLayout: TextLayoutResult) {
        val previous = layouts[token]
        if (previous?.textLayout === textLayout) return
        layouts[token] = (previous ?: BookDocumentSelectionLayout()).copy(textLayout = textLayout)
        if (hasSelection) layoutRevision++
    }

    fun updateTextPosition(token: String, positionInWindow: IntOffset) {
        val previous = layouts[token]
        if (previous?.positionInWindow == positionInWindow) return
        layouts[token] = (previous ?: BookDocumentSelectionLayout()).copy(
            positionInWindow = positionInWindow,
        )
        if (hasSelection) layoutRevision++
    }

    fun clearTextPosition(token: String) {
        val previous = layouts[token]?.takeIf { it.positionInWindow != null } ?: return
        layouts[token] = previous.copy(positionInWindow = null)
        if (hasSelection) layoutRevision++
    }

    fun captureSelectionAtPointerDown() {
        selectionPresentAtPointerDown = hasSelection
    }

    fun consumeSelectionTap(): Boolean {
        val shouldConsume = selectionPresentAtPointerDown || hasSelection
        selectionPresentAtPointerDown = false
        if (shouldConsume) clearSelection()
        return shouldConsume
    }

    fun handleReaderTap(onReaderTap: () -> Unit) {
        if (!consumeSelectionTap()) onReaderTap()
    }

    fun clearSelection() {
        selectionState.clear()
        restrictedChapterId = null
        geometryTokens = emptySet()
    }

    fun allowsSelection(chapterId: Long): Boolean =
        restrictedChapterId == null || restrictedChapterId == chapterId

    fun updateOwnerIdentity(ownerIdentity: String) {
        if (this.ownerIdentity == ownerIdentity) return
        clearSelection()
        clearPublishedSelection()
        this.ownerIdentity = ownerIdentity
    }

    fun performAction(action: BookDocumentSelectionAction) {
        val projection = project(selectionState.selectedTexts, interaction.rootPositionInWindow) ?: return
        publishSelection(projection)
        interaction.onSelectionAction(ownerIdentity, projection.identity, action)
    }

    fun publishSelection(projection: BookDocumentSelectionProjection?) {
        if (!interaction.observeSelections) {
            clearPublishedSelection()
            return
        }
        if (projection == null) {
            restrictedChapterId = null
            geometryTokens = emptySet()
            clearPublishedSelection()
            return
        }
        restrictedChapterId = projection.chapterId
        geometryTokens = projection.selectedTokens
        val bounds = projection.boundsInReaderRoot ?: RectF()
        if (bounds.isEmpty) return
        interaction.onSelection(
            BookDocumentTextSelection.Changed(
                ownerIdentity = ownerIdentity,
                identity = projection.identity,
                text = projection.text,
                languageContextText = projection.languageContextText,
                boundsInReaderRoot = bounds,
            ),
        )
    }

    fun clearPublishedSelection() {
        interaction.onSelection(BookDocumentTextSelection.Cleared(ownerIdentity))
    }

    fun project(
        selectedTexts: List<AnnotatedString>,
        readerRootPositionInWindow: Offset,
    ): BookDocumentSelectionProjection? = projectBookDocumentSelection(
        ownerIdentity = ownerIdentity,
        selectedTexts = selectedTexts,
        selectableLeaves = selectableLeaves,
        layouts = layouts,
        readerRootPositionInWindow = readerRootPositionInWindow,
    )
}

internal val LocalBookDocumentChapterSelection = compositionLocalOf<BookDocumentChapterSelection?> { null }
internal val LocalBookDocumentSelectionChapterId = compositionLocalOf<Long?> { null }
