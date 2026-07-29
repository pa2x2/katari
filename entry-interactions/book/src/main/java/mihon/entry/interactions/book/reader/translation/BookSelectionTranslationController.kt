package mihon.entry.interactions.book

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import mihon.entry.viewer.settings.ReaderCapabilityId
import mihon.entry.viewer.settings.StandardReaderCapabilities
import mihon.translation.api.TranslationDeviceAvailability
import mihon.translation.api.TranslationEngineSelection
import mihon.translation.api.TranslationFeature
import mihon.translation.api.TranslationHostActions
import mihon.translation.api.TranslationRequest
import mihon.translation.api.TranslationSourceLanguageSelection
import mihon.translation.api.TranslationTargetLanguageSelection
import mihon.translation.ui.session.TranslationSelectionAnchor
import mihon.translation.ui.session.TranslationSessionHostCoordinator
import mihon.translation.ui.session.TranslationSessionInput
import mihon.translation.ui.session.TranslationSessionState
import tachiyomi.core.common.preference.Preference

internal data class BookReaderTextSelection(
    val ownerIdentity: String,
    val identity: String,
    val text: String,
    val anchor: TranslationSelectionAnchor?,
) {
    init {
        require(ownerIdentity.isNotBlank())
        require(identity.isNotBlank())
        require(text.isNotBlank())
    }
}

internal class BookSelectionTranslationController(
    feature: TranslationFeature,
    private val hostActions: TranslationHostActions,
    private val automaticSelectionEnabled: Preference<Boolean>,
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
    private var dismissedSelectionIdentity: String? = null
    private var availabilityJob: Job? = null
    private val observerJobs = listOf(
        automaticSelectionEnabled.changes()
            .onEach {
                clearSelection()
                updateEffectiveState()
            }
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
        if (dismissedSelectionIdentity == selection.identity) return

        val previous = currentSelection
        currentSelection = selection
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
                ),
                anchor = selection.anchor,
            ),
        )
    }

    fun clearSelection() {
        currentSelection = null
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

    fun dismissTranslationOnReaderTap(): Boolean {
        if (hostCoordinator.controller.state.value !is TranslationSessionState.Active) return false
        dismissTranslation()
        return true
    }

    override fun close() {
        if (closed) return
        closed = true
        availabilityJob?.cancel()
        observerJobs.forEach(Job::cancel)
        currentSelection = null
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
        val enabled = automaticSelectionEnabled.get() &&
            capable &&
            mutableDeviceAvailability.value == TranslationDeviceAvailability.Available
        if (mutableEffectiveEnabled.value == enabled) return
        mutableEffectiveEnabled.value = enabled
        if (!enabled) clearSelection()
    }

    private companion object {
        val REQUIRED_CAPABILITIES = setOf(
            StandardReaderCapabilities.StableTextSelection,
            StandardReaderCapabilities.SelectionAnchoring,
        )
    }
}
