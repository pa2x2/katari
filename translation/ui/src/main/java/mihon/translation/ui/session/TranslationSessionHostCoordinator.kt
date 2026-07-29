package mihon.translation.ui.session

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import mihon.translation.api.TranslationEngineId
import mihon.translation.api.TranslationEngineSelection
import mihon.translation.api.TranslationEngineState
import mihon.translation.api.TranslationEngineStatus
import mihon.translation.api.TranslationFeature
import mihon.translation.api.TranslationHostActionResult
import mihon.translation.api.TranslationHostActions
import mihon.translation.api.TranslationLanguageTag
import mihon.translation.api.TranslationPreparation
import mihon.translation.api.TranslationSourceLanguageSelection
import mihon.translation.api.TranslationTargetLanguageSelection
import mihon.translation.api.TranslationUnavailableReason
import mihon.translation.ui.picker.TranslationLanguageRole
import mihon.translation.ui.picker.supportsPair
import mihon.translation.ui.picker.supportsSelection
import mihon.translation.ui.presentation.TranslationSessionExternalAction

class TranslationSessionHostCoordinator(
    feature: TranslationFeature,
    private val hostActions: TranslationHostActions,
    private val scope: CoroutineScope,
    executionMode: TranslationSessionExecutionMode = TranslationSessionExecutionMode.FollowProviderPolicy,
    selectionSettleDelayMillis: Long = 250L,
) {
    private val mutableEngineStates = MutableStateFlow(
        hostActions.knownEngines.map { engine ->
            TranslationEngineState(
                engine = engine,
                presentation = null,
                status = TranslationEngineStatus.Checking,
            )
        },
    )
    val engineStates: StateFlow<List<TranslationEngineState>> = mutableEngineStates.asStateFlow()
    private var resolvedProfileSelectedEngine =
        hostActions.selectedEngine.get().takeIf { hostActions.selectedEngine.isSet() }
    val profileSelectedEngine: TranslationEngineId?
        get() = resolvedProfileSelectedEngine

    val controller = TranslationSessionController(
        feature = feature,
        parentScope = scope,
        executionMode = executionMode,
        selectionSettleDelayMillis = selectionSettleDelayMillis,
    )
    private val languageSupportController = TranslationLanguageSupportController(
        hostActions = hostActions,
        scope = scope,
    )
    val languageSupport: StateFlow<TranslationLanguageSupportState> = languageSupportController.state

    private val mutablePicker = MutableStateFlow<TranslationSessionPicker?>(null)
    val picker: StateFlow<TranslationSessionPicker?> = mutablePicker.asStateFlow()

    private val mutableResults = MutableSharedFlow<TranslationHostActionResult>(extraBufferCapacity = 1)
    val results: SharedFlow<TranslationHostActionResult> = mutableResults.asSharedFlow()

    private var actionJob: Job? = null
    private var retryAfterResume = false
    private var pickerSourceLanguage: TranslationLanguageTag? = null
    private var pickerTargetLanguage: TranslationLanguageTag? = null

    init {
        refreshEngineStates()
    }

    fun handleExternalAction(
        action: TranslationSessionExternalAction,
        openDocumentation: (String) -> Unit,
    ) {
        when (action) {
            TranslationSessionExternalAction.ChooseSourceLanguage -> openLanguagePicker(
                TranslationSessionPicker.SourceLanguage,
            )
            TranslationSessionExternalAction.ChooseTargetLanguage -> openLanguagePicker(
                TranslationSessionPicker.TargetLanguage,
            )
            is TranslationSessionExternalAction.ChangeLanguages -> {
                pickerSourceLanguage = action.source
                pickerTargetLanguage = action.target
                openLanguagePicker(
                    picker = TranslationSessionPicker.TargetLanguage,
                    retainLanguageContext = true,
                )
            }
            TranslationSessionExternalAction.ChooseEngine ->
                mutablePicker.value = TranslationSessionPicker.Engine
            is TranslationSessionExternalAction.ConfirmProviderDisclosure -> performAction {
                hostActions.acknowledgeProviderDisclosure(action.engine, action.disclosure)
            }
            is TranslationSessionExternalAction.DownloadModels -> performAction {
                hostActions.downloadModels(action.engine, action.models)
            }
            is TranslationSessionExternalAction.OpenSetup -> performAction {
                hostActions.openSetup(action.engine)
            }
            is TranslationSessionExternalAction.OpenDocumentation -> openDocumentation(action.url)
        }
    }

    fun selectLanguage(language: TranslationLanguageTag) {
        val available = languageSupport.value as? TranslationLanguageSupportState.Available
            ?: return
        if (available.engine != activeEngine()) return
        when (mutablePicker.value) {
            TranslationSessionPicker.SourceLanguage -> {
                if (!available.support.supportsSelection(
                        TranslationLanguageRole.Source,
                        language,
                        pickerTargetLanguage,
                    )
                ) {
                    return
                }
                pickerSourceLanguage = language
                controller.selectSourceLanguage(language)
            }
            TranslationSessionPicker.TargetLanguage -> {
                if (!available.support.supportsSelection(
                        TranslationLanguageRole.Target,
                        language,
                        pickerSourceLanguage,
                    )
                ) {
                    return
                }
                pickerTargetLanguage = language
                controller.selectTargetLanguage(language)
            }
            TranslationSessionPicker.Engine,
            null,
            -> return
        }
        mutablePicker.value = null
    }

    fun selectEngine(engine: TranslationEngineId) {
        if (mutableEngineStates.value.none { it.engine.id == engine && it.status == TranslationEngineStatus.Ready }) {
            return
        }
        controller.selectEngine(TranslationEngineSelection.Explicit(engine))
        languageSupportController.load(engine)
        mutablePicker.value = null
    }

    fun openEngineSetup(engine: TranslationEngineId) {
        performAction {
            hostActions.openSetup(engine)
        }
    }

    fun useCurrentTargetAsProfileDefault() {
        val source = pickerSourceLanguage ?: return
        val target = pickerTargetLanguage ?: return
        val available = languageSupport.value as? TranslationLanguageSupportState.Available
            ?: return
        if (available.engine != activeEngine() || !available.support.supportsPair(source, target)) return
        hostActions.setDefaultTargetLanguage(target)
        mutableResults.tryEmit(TranslationHostActionResult.Completed)
    }

    fun canUseCurrentTargetAsProfileDefault(): Boolean {
        val source = pickerSourceLanguage ?: return false
        val target = pickerTargetLanguage ?: return false
        val available = languageSupport.value as? TranslationLanguageSupportState.Available
            ?: return false
        return available.engine == activeEngine() && available.support.supportsPair(source, target)
    }

    fun selectedLanguage(picker: TranslationSessionPicker): TranslationLanguageTag? =
        when (picker) {
            TranslationSessionPicker.SourceLanguage -> pickerSourceLanguage
            TranslationSessionPicker.TargetLanguage -> pickerTargetLanguage
            TranslationSessionPicker.Engine -> null
        }

    fun counterpartLanguage(picker: TranslationSessionPicker): TranslationLanguageTag? =
        when (picker) {
            TranslationSessionPicker.SourceLanguage -> pickerTargetLanguage
            TranslationSessionPicker.TargetLanguage -> pickerSourceLanguage
            TranslationSessionPicker.Engine -> null
        }

    fun activeEngine(): TranslationEngineId? {
        val active = controller.state.value as? TranslationSessionState.Active
        return when (val selection = active?.input?.request?.engine) {
            is TranslationEngineSelection.Explicit -> selection.engine
            TranslationEngineSelection.ProfileDefault,
            null,
            -> profileSelectedEngine
        }
    }

    fun retryLanguageSupport() {
        languageSupportController.retry()
    }

    fun dismissPicker() {
        mutablePicker.value = null
    }

    fun onResume() {
        refreshEngineStates()
        if (mutablePicker.value != null && mutablePicker.value != TranslationSessionPicker.Engine) {
            languageSupportController.load(activeEngine())
        }
        if (!retryAfterResume) return
        retryAfterResume = false
        controller.retry()
    }

    fun close() {
        actionJob?.cancel()
        mutablePicker.value = null
        languageSupportController.clear()
        controller.close()
    }

    private fun performAction(action: suspend () -> TranslationHostActionResult) {
        actionJob?.cancel()
        actionJob = scope.launch {
            val result = try {
                action()
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                TranslationHostActionResult.Failed("Unexpected translation host failure")
            }
            when (result) {
                TranslationHostActionResult.Completed,
                TranslationHostActionResult.ModelsReady,
                -> controller.retry()
                is TranslationHostActionResult.SetupOpened -> retryAfterResume = true
                is TranslationHostActionResult.ModelsFailed,
                TranslationHostActionResult.SetupUnsupported,
                TranslationHostActionResult.ServiceMissing,
                TranslationHostActionResult.SettingsUnavailable,
                is TranslationHostActionResult.Failed,
                -> Unit
            }
            mutableResults.emit(result)
            refreshEngineStates()
        }
    }

    private fun refreshEngineStates() {
        scope.launch {
            val inspection = hostActions.inspectEngines()
            resolvedProfileSelectedEngine = inspection.selectedEngine
            mutableEngineStates.value = inspection.engines
        }
    }

    private fun openLanguagePicker(
        picker: TranslationSessionPicker,
        retainLanguageContext: Boolean = false,
    ) {
        if (!retainLanguageContext) {
            val context = resolvedLanguageContext()
            pickerSourceLanguage = context.first
            pickerTargetLanguage = context.second
        }
        languageSupportController.load(activeEngine())
        mutablePicker.value = picker
    }

    private fun resolvedLanguageContext(): Pair<TranslationLanguageTag?, TranslationLanguageTag?> {
        val state = controller.state.value as? TranslationSessionState.Active
            ?: return null to null
        val explicitSource =
            (state.input.request.sourceLanguage as? TranslationSourceLanguageSelection.Explicit)?.language
        val explicitTarget =
            (state.input.request.targetLanguage as? TranslationTargetLanguageSelection.Explicit)?.language
        return when (state) {
            is TranslationSessionState.Ready ->
                state.preparation.request.sourceLanguage to state.preparation.request.targetLanguage
            is TranslationSessionState.Success ->
                state.result.sourceLanguage to state.result.targetLanguage
            is TranslationSessionState.Settling ->
                state.previousResult?.let { it.sourceLanguage to it.targetLanguage }
                    ?: (explicitSource to explicitTarget)
            is TranslationSessionState.Preparing ->
                state.previousResult?.let { it.sourceLanguage to it.targetLanguage }
                    ?: (explicitSource to explicitTarget)
            is TranslationSessionState.Translating ->
                state.previousResult?.let { it.sourceLanguage to it.targetLanguage }
                    ?: (explicitSource to explicitTarget)
            is TranslationSessionState.PreparationRequired -> when (val preparation = state.preparation) {
                is TranslationPreparation.TargetLanguageRequired ->
                    (preparation.sourceLanguage ?: explicitSource) to explicitTarget
                is TranslationPreparation.Unavailable -> {
                    val pair = preparation.reason as? TranslationUnavailableReason.UnsupportedLanguagePair
                    (pair?.source ?: explicitSource) to (pair?.target ?: explicitTarget)
                }
                else -> explicitSource to explicitTarget
            }
            is TranslationSessionState.ProviderSurfaceOpened,
            is TranslationSessionState.Failed,
            -> explicitSource to explicitTarget
        }
    }
}

enum class TranslationSessionPicker {
    SourceLanguage,
    TargetLanguage,
    Engine,
}
