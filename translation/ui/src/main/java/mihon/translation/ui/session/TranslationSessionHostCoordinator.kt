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
import mihon.translation.api.TranslationFeature
import mihon.translation.api.TranslationHostActionResult
import mihon.translation.api.TranslationHostActions
import mihon.translation.api.TranslationLanguageTag
import mihon.translation.api.TranslationTargetLanguageSelection
import mihon.translation.ui.presentation.TranslationSessionExternalAction

class TranslationSessionHostCoordinator(
    feature: TranslationFeature,
    private val hostActions: TranslationHostActions,
    private val scope: CoroutineScope,
    executionMode: TranslationSessionExecutionMode = TranslationSessionExecutionMode.FollowProviderPolicy,
    selectionSettleDelayMillis: Long = 250L,
) {
    val knownEngines = hostActions.knownEngines
    val profileSelectedEngine: TranslationEngineId
        get() = hostActions.selectedEngine.get()

    val controller = TranslationSessionController(
        feature = feature,
        parentScope = scope,
        executionMode = executionMode,
        selectionSettleDelayMillis = selectionSettleDelayMillis,
    )

    private val mutablePicker = MutableStateFlow<TranslationSessionPicker?>(null)
    val picker: StateFlow<TranslationSessionPicker?> = mutablePicker.asStateFlow()

    private val mutableResults = MutableSharedFlow<TranslationHostActionResult>(extraBufferCapacity = 1)
    val results: SharedFlow<TranslationHostActionResult> = mutableResults.asSharedFlow()

    private var actionJob: Job? = null
    private var retryAfterResume = false

    fun handleExternalAction(
        action: TranslationSessionExternalAction,
        openDocumentation: (String) -> Unit,
    ) {
        when (action) {
            TranslationSessionExternalAction.ChooseSourceLanguage ->
                mutablePicker.value = TranslationSessionPicker.SourceLanguage
            TranslationSessionExternalAction.ChooseTargetLanguage,
            is TranslationSessionExternalAction.ChangeLanguages,
            -> mutablePicker.value = TranslationSessionPicker.TargetLanguage
            TranslationSessionExternalAction.ChooseEngine ->
                mutablePicker.value = TranslationSessionPicker.Engine
            is TranslationSessionExternalAction.ConfirmProviderDisclosure -> performAction {
                hostActions.acknowledgeProviderDisclosure(action.engine, action.disclosure)
            }
            is TranslationSessionExternalAction.DownloadModels -> performAction {
                hostActions.downloadModels(action.engine, action.models)
            }
            is TranslationSessionExternalAction.OpenSystemSetup -> performAction {
                hostActions.openSystemSetup(action.engine)
            }
            is TranslationSessionExternalAction.OpenDocumentation -> openDocumentation(action.url)
        }
    }

    fun selectLanguage(language: TranslationLanguageTag) {
        when (mutablePicker.value) {
            TranslationSessionPicker.SourceLanguage -> controller.selectSourceLanguage(language)
            TranslationSessionPicker.TargetLanguage -> controller.selectTargetLanguage(language)
            TranslationSessionPicker.Engine,
            null,
            -> return
        }
        mutablePicker.value = null
    }

    fun selectEngine(engine: TranslationEngineId) {
        controller.selectEngine(TranslationEngineSelection.Explicit(engine))
        mutablePicker.value = null
    }

    fun useCurrentTargetAsProfileDefault() {
        val active = controller.state.value as? TranslationSessionState.Active ?: return
        val target = active.input.request.targetLanguage as? TranslationTargetLanguageSelection.Explicit ?: return
        hostActions.setDefaultTargetLanguage(target.language)
        mutableResults.tryEmit(TranslationHostActionResult.Completed)
    }

    fun dismissPicker() {
        mutablePicker.value = null
    }

    fun onResume() {
        if (!retryAfterResume) return
        retryAfterResume = false
        controller.retry()
    }

    fun close() {
        actionJob?.cancel()
        mutablePicker.value = null
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
                TranslationHostActionResult.SystemSetupOpened -> retryAfterResume = true
                is TranslationHostActionResult.ModelsFailed,
                TranslationHostActionResult.SetupUnsupported,
                TranslationHostActionResult.ServiceMissing,
                TranslationHostActionResult.SettingsUnavailable,
                is TranslationHostActionResult.Failed,
                -> Unit
            }
            mutableResults.emit(result)
        }
    }
}

enum class TranslationSessionPicker {
    SourceLanguage,
    TargetLanguage,
    Engine,
}
