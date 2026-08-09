package mihon.entry.interactions.book.reader.selection

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import mihon.entry.interactions.book.document.reader.BookDocumentSelectionAction
import mihon.entry.interactions.book.document.reader.BookDocumentTextSelection
import mihon.entry.interactions.book.reader.translation.BookSelectionTranslationController
import mihon.entry.viewer.settings.shared.ReaderCapabilityId
import mihon.entry.viewer.settings.shared.StandardReaderCapabilities

internal class BookSelectionActionCoordinator(
    val translationController: BookSelectionTranslationController,
    private val speechController: BookSelectionSpeechController,
    scope: CoroutineScope,
    initialCapabilities: Set<ReaderCapabilityId>,
) : AutoCloseable {
    private val mutableObserveSelections = MutableStateFlow(false)
    val observeSelections: StateFlow<Boolean> = mutableObserveSelections.asStateFlow()
    val automaticTranslationEnabled: StateFlow<Boolean> = translationController.effectiveEnabled
    val speechState: StateFlow<BookSelectionSpeechState> = speechController.state

    private var capabilities = initialCapabilities
    private var currentSelection: BookReaderTextSelection? = null
    private var closed = false
    private val automaticTranslationJob: Job = translationController.effectiveEnabled
        .onEach { enabled ->
            if (enabled && !closed) currentSelection?.let(translationController::submitSelection)
        }
        .launchIn(scope)

    init {
        updateObservationState()
    }

    fun updateCapabilities(capabilities: Set<ReaderCapabilityId>) {
        if (this.capabilities == capabilities) return
        this.capabilities = capabilities
        translationController.updateCapabilities(capabilities)
        updateObservationState()
    }

    fun onResume() {
        translationController.onResume()
    }

    fun onStop() {
        speechController.stopPlayback()
    }

    fun submitSelection(selection: BookDocumentTextSelection.Changed) {
        if (!mutableObserveSelections.value || closed) return
        val next = selection.toReaderSelection()
        val previous = currentSelection
        currentSelection = next

        if (previous?.identity == next.identity && previous.text == next.text) {
            translationController.updateSelectionAnchor(next)
            return
        }

        speechController.onSelectionChanged(next.identity)
        translationController.onSelectionChanged(next)
        translationController.submitSelection(next)
    }

    fun clearSelection(ownerIdentity: String) {
        val selection = currentSelection?.takeIf { it.ownerIdentity == ownerIdentity } ?: return
        currentSelection = null
        speechController.clearSelection(selection.identity)
        translationController.clearSelection(ownerIdentity)
    }

    fun performAction(
        ownerIdentity: String,
        selectionIdentity: String,
        action: BookDocumentSelectionAction,
    ) {
        val selection = currentSelection?.takeIf {
            it.ownerIdentity == ownerIdentity && it.identity == selectionIdentity
        } ?: return
        when (action) {
            BookDocumentSelectionAction.Listen -> speechController.toggle(selection)
            BookDocumentSelectionAction.Translate -> translationController.translateSelection(selection)
        }
    }

    fun isTranslationActive(): Boolean = translationController.isTranslationActive()

    fun dismissTranslation() {
        translationController.dismissTranslation()
    }

    fun dismissTranslationOnReaderTap(): Boolean = translationController.dismissTranslationOnReaderTap()

    override fun close() {
        if (closed) return
        closed = true
        currentSelection = null
        mutableObserveSelections.value = false
        automaticTranslationJob.cancel()
        speechController.close()
        translationController.close()
    }

    private fun updateObservationState() {
        val enabled = !closed && capabilities.containsAll(REQUIRED_CAPABILITIES)
        if (mutableObserveSelections.value == enabled) return
        mutableObserveSelections.value = enabled
        if (!enabled) {
            currentSelection = null
            speechController.clearSelection()
            translationController.clearSelection()
        }
    }

    private fun BookDocumentTextSelection.Changed.toReaderSelection() = BookReaderTextSelection(
        ownerIdentity = ownerIdentity,
        identity = identity,
        text = text,
        anchor = boundsInReaderRoot.let { bounds ->
            BookReaderTextSelectionAnchor(bounds.left, bounds.top, bounds.right, bounds.bottom)
        },
    )

    private companion object {
        val REQUIRED_CAPABILITIES = setOf(
            StandardReaderCapabilities.StableTextSelection,
            StandardReaderCapabilities.SelectionAnchoring,
        )
    }
}
