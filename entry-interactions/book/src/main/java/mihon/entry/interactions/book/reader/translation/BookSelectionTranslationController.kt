package mihon.entry.interactions.book.reader.translation

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import mihon.entry.interactions.book.reader.language.BookSelectionLanguageSession
import mihon.entry.viewer.settings.ResolvedViewerSetting
import mihon.entry.viewer.settings.shared.ReaderCapabilityId
import mihon.entry.viewer.settings.shared.StandardReaderCapabilities
import mihon.translation.api.TranslationFeature
import mihon.translation.api.availability.TranslationDeviceAvailability
import mihon.translation.api.engine.TranslationEngineSelection
import mihon.translation.api.host.TranslationHostActions
import mihon.translation.api.request.TranslationRequest
import mihon.translation.api.request.TranslationSourceLanguageSelection
import mihon.translation.api.request.TranslationTargetLanguageSelection
import mihon.translation.ui.session.TranslationSelectionAnchor
import mihon.translation.ui.session.TranslationSessionHostCoordinator
import mihon.translation.ui.session.TranslationSessionInput
import mihon.translation.ui.session.TranslationSessionState
import mihon.entry.interactions.book.reader.selection.BookReaderTextSelection as NeutralBookReaderTextSelection

internal data class BookReaderTextSelection(
    val ownerIdentity: String,
    val identity: String,
    val text: String,
    val languageContextText: String,
    val anchor: TranslationSelectionAnchor?,
) {
    init {
        require(ownerIdentity.isNotBlank())
        require(identity.isNotBlank())
        require(text.isNotBlank())
        require(languageContextText.isNotBlank())
    }
}

internal class BookSelectionTranslationController(
    feature: TranslationFeature,
    private val hostActions: TranslationHostActions,
    private val automaticSelectionSetting: StateFlow<ResolvedViewerSetting<Boolean>>,
    private val languageSession: BookSelectionLanguageSession,
    private val scope: CoroutineScope,
    initialCapabilities: Set<ReaderCapabilityId>,
) : AutoCloseable {
    val hostCoordinator = TranslationSessionHostCoordinator(
        feature = feature,
        hostActions = hostActions,
        scope = scope,
    )

    private val mutableEffectiveEnabled = MutableStateFlow(false)
    val effectiveEnabled: StateFlow<Boolean> = mutableEffectiveEnabled.asStateFlow()

    private val mutableDeviceAvailability =
        MutableStateFlow<TranslationDeviceAvailability?>(null)
    val deviceAvailability: StateFlow<TranslationDeviceAvailability?> =
        mutableDeviceAvailability.asStateFlow()

    private var capabilities = initialCapabilities
    private var currentSelection: BookReaderTextSelection? = null
    private var currentSelectionWasAutomatic = false
    private var dismissedSelectionIdentity: String? = null
    private var availabilityJob: Job? = null
    private val observerJobs = listOf(
        automaticSelectionSetting
            .onEach { updateEffectiveState() }
            .launchIn(scope),
        hostActions.selectedEngine.changes()
            .onEach {
                clearSelection()
                refreshDeviceAvailability()
            }
            .launchIn(scope),
        hostActions.defaultTargetLanguage.changes()
            .onEach { clearSelection() }
            .launchIn(scope),
    )
    private var closed = false

    init {
        refreshDeviceAvailability()
    }

    fun updateCapabilities(capabilities: Set<ReaderCapabilityId>) {
        if (this.capabilities == capabilities) return
        this.capabilities = capabilities
        updateEffectiveState()
    }

    fun onResume() {
        hostCoordinator.onResume()
        refreshDeviceAvailability()
    }

    fun submitSelection(selection: BookReaderTextSelection) {
        if (!mutableEffectiveEnabled.value || closed) return
        submit(selection, automatic = true)
    }

    fun submitSelection(selection: NeutralBookReaderTextSelection) {
        if (!mutableEffectiveEnabled.value || closed) return
        submit(selection.toTranslationSelection(), automatic = true)
    }

    fun translateSelection(selection: NeutralBookReaderTextSelection) {
        if (closed) return
        dismissedSelectionIdentity = null
        submit(selection.toTranslationSelection(), automatic = false)
    }

    fun onSelectionChanged(selection: NeutralBookReaderTextSelection) {
        val translatedSelection = currentSelection ?: return
        if (translatedSelection.identity == selection.identity && translatedSelection.text == selection.text) {
            updateSelectionAnchor(selection)
        } else {
            clearSelection()
        }
    }

    fun updateSelectionAnchor(selection: NeutralBookReaderTextSelection) {
        val translatedSelection = currentSelection ?: return
        if (translatedSelection.identity != selection.identity || translatedSelection.text != selection.text) return
        currentSelection = selection.toTranslationSelection()
        hostCoordinator.controller.updateAnchor(currentSelection?.anchor)
    }

    private fun submit(selection: BookReaderTextSelection, automatic: Boolean) {
        if (dismissedSelectionIdentity == selection.identity) return

        val previous = currentSelection
        currentSelection = selection
        currentSelectionWasAutomatic = automatic
        if (previous?.identity == selection.identity && previous.text == selection.text) {
            hostCoordinator.controller.updateAnchor(selection.anchor)
            return
        }
        dismissedSelectionIdentity = null
        hostCoordinator.controller.submit(
            TranslationSessionInput(
                request = TranslationRequest(
                    text = selection.text,
                    sourceLanguage = TranslationSourceLanguageSelection.Automatic,
                    targetLanguage = TranslationTargetLanguageSelection.Default,
                    engine = TranslationEngineSelection.ProfileDefault,
                    languageContext = languageSession.context(selection.languageContextText),
                ),
                anchor = selection.anchor,
            ),
        )
    }

    fun clearSelection() {
        currentSelection = null
        currentSelectionWasAutomatic = false
        dismissedSelectionIdentity = null
        hostCoordinator.controller.clear()
    }

    fun clearSelection(ownerIdentity: String) {
        if (currentSelection?.ownerIdentity == ownerIdentity) clearSelection()
    }

    fun dismissTranslation() {
        dismissedSelectionIdentity = currentSelection?.identity
        hostCoordinator.controller.dismiss()
    }

    fun isTranslationActive(): Boolean {
        return hostCoordinator.controller.state.value is TranslationSessionState.Active
    }

    fun dismissTranslationOnReaderTap(): Boolean {
        if (!isTranslationActive()) return false
        dismissTranslation()
        return true
    }

    override fun close() {
        if (closed) return
        closed = true
        availabilityJob?.cancel()
        observerJobs.forEach(Job::cancel)
        currentSelection = null
        currentSelectionWasAutomatic = false
        dismissedSelectionIdentity = null
        mutableEffectiveEnabled.value = false
        hostCoordinator.close()
    }

    private fun refreshDeviceAvailability() {
        availabilityJob?.cancel()
        availabilityJob = scope.launch {
            mutableDeviceAvailability.value = hostActions.deviceAvailability()
            updateEffectiveState()
        }
    }

    private fun updateEffectiveState() {
        val capable = capabilities.containsAll(REQUIRED_CAPABILITIES)
        val enabled = automaticSelectionSetting.value.effectiveValue &&
            capable &&
            mutableDeviceAvailability.value == TranslationDeviceAvailability.Available
        if (mutableEffectiveEnabled.value == enabled) return
        mutableEffectiveEnabled.value = enabled
        if (!enabled && currentSelectionWasAutomatic) clearSelection()
    }

    private companion object {
        val REQUIRED_CAPABILITIES = setOf(
            StandardReaderCapabilities.StableTextSelection,
            StandardReaderCapabilities.SelectionAnchoring,
        )
    }
}

private fun NeutralBookReaderTextSelection.toTranslationSelection() = BookReaderTextSelection(
    ownerIdentity = ownerIdentity,
    identity = identity,
    text = text,
    languageContextText = languageContextText,
    anchor = anchor?.let { anchor ->
        TranslationSelectionAnchor(
            left = anchor.left,
            top = anchor.top,
            right = anchor.right,
            bottom = anchor.bottom,
        )
    },
)
