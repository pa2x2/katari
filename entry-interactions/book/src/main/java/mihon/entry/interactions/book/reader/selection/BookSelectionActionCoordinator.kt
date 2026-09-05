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
import mihon.entry.interactions.book.reader.language.BookSelectionLanguageSession
import mihon.entry.interactions.book.reader.speech.BookShortFormSpeechController
import mihon.entry.interactions.book.reader.speech.BookShortFormSpeechOwner
import mihon.entry.interactions.book.reader.speech.BookShortFormSpeechRequest
import mihon.entry.interactions.book.reader.speech.BookShortFormSpeechState
import mihon.entry.interactions.book.reader.translation.BookSelectionTranslationController
import mihon.entry.viewer.settings.shared.ReaderCapabilityId
import mihon.entry.viewer.settings.shared.StandardReaderCapabilities
import mihon.translation.ui.presentation.TranslationResultSpeechSide
import mihon.translation.ui.presentation.TranslationResultSpeechTarget
import mihon.translation.ui.session.TranslationSessionState
import mihon.tts.api.request.TtsLanguageSelection

internal class BookSelectionActionCoordinator(
    val translationController: BookSelectionTranslationController,
    private val speechController: BookShortFormSpeechController,
    private val languageSession: BookSelectionLanguageSession,
    scope: CoroutineScope,
    initialCapabilities: Set<ReaderCapabilityId>,
) : AutoCloseable {
    private val mutableObserveSelections = MutableStateFlow(false)
    val observeSelections: StateFlow<Boolean> = mutableObserveSelections.asStateFlow()
    val automaticTranslationEnabled: StateFlow<Boolean> = translationController.effectiveEnabled
    val speechState: StateFlow<BookShortFormSpeechState> = speechController.state

    private var capabilities = initialCapabilities
    private var currentSelection: BookReaderTextSelection? = null
    private var closed = false
    private val automaticTranslationJob: Job = translationController.effectiveEnabled
        .onEach { enabled ->
            if (enabled && !closed) currentSelection?.let(translationController::submitSelection)
        }
        .launchIn(scope)
    private val translationSpeechJob: Job = translationController.hostCoordinator.controller.state
        .onEach { state ->
            if (state is TranslationSessionState.Success) {
                languageSession.record(state.result.sourceLanguage)
            }
            val active = speechController.state.value.owner as? BookShortFormSpeechOwner.TranslationResult
                ?: return@onEach
            if (active.target !in state.speechTargets()) speechController.clearTranslationResult()
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
            if (previous.isSettled != next.isSettled) {
                translationController.submitSelection(next)
            }
            return
        }

        speechController.onSelectionChanged(next.identity)
        speechController.clearTranslationResult()
        translationController.onSelectionChanged(next)
        translationController.submitSelection(next)
    }

    fun clearSelection(ownerIdentity: String) {
        val selection = currentSelection?.takeIf { it.ownerIdentity == ownerIdentity } ?: return
        currentSelection = null
        speechController.clearSelection(selection.identity)
        speechController.clearTranslationResult()
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
            BookDocumentSelectionAction.Listen -> speechController.toggle(
                BookShortFormSpeechRequest(
                    owner = BookShortFormSpeechOwner.Selection(selection.identity),
                    text = selection.text,
                    language = TtsLanguageSelection.Automatic,
                    languageContext = languageSession.context(selection.languageContextText),
                ),
            )
            BookDocumentSelectionAction.Translate -> translationController.translateSelection(selection)
        }
    }

    fun toggleTranslationSpeech(target: TranslationResultSpeechTarget) {
        if (target !in translationController.hostCoordinator.controller.state.value.speechTargets()) return
        speechController.toggle(
            BookShortFormSpeechRequest(
                owner = BookShortFormSpeechOwner.TranslationResult(target),
                text = target.text,
                language = TtsLanguageSelection.Explicit(target.language),
            ),
        )
    }

    fun isTranslationActive(): Boolean = translationController.isTranslationActive()

    fun dismissTranslation() {
        speechController.clearTranslationResult()
        translationController.dismissTranslation()
    }

    fun dismissTranslationOnReaderTap(): Boolean {
        if (!translationController.isTranslationActive()) return false
        dismissTranslation()
        return true
    }

    override fun close() {
        if (closed) return
        closed = true
        currentSelection = null
        mutableObserveSelections.value = false
        automaticTranslationJob.cancel()
        translationSpeechJob.cancel()
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
            speechController.clearTranslationResult()
            translationController.clearSelection()
        }
    }

    private fun BookDocumentTextSelection.Changed.toReaderSelection() = BookReaderTextSelection(
        ownerIdentity = ownerIdentity,
        identity = identity,
        text = text,
        languageContextText = languageContextText,
        isSettled = isSettled,
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

private fun TranslationSessionState.speechTargets(): Set<TranslationResultSpeechTarget> {
    val success = this as? TranslationSessionState.Success ?: return emptySet()
    return setOf(
        TranslationResultSpeechTarget(
            side = TranslationResultSpeechSide.Source,
            text = success.input.request.text,
            language = success.result.sourceLanguage,
        ),
        TranslationResultSpeechTarget(
            side = TranslationResultSpeechSide.Target,
            text = success.result.translatedText,
            language = success.result.targetLanguage,
        ),
    )
}
