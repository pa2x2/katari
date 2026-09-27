package mihon.translation.ui.session

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import mihon.language.api.identification.TextLanguageResolutionContext
import mihon.language.api.tag.LanguageTag
import mihon.translation.api.TranslationFeature
import mihon.translation.api.engine.TranslationEngineId
import mihon.translation.api.engine.TranslationEngineSelection
import mihon.translation.api.engine.TranslationEngineStatus
import mihon.translation.api.host.TranslationHostActionResult
import mihon.translation.api.host.TranslationHostActions
import mihon.translation.api.language.TranslationLanguageSupport
import mihon.translation.api.preparation.TranslationPreparation
import mihon.translation.api.preparation.TranslationUnavailableReason
import mihon.translation.api.request.TranslationSourceLanguageSelection
import mihon.translation.api.request.TranslationTargetLanguageSelection
import mihon.translation.ui.picker.language.TranslationLanguageRole
import mihon.translation.ui.picker.language.supportsSelection
import mihon.translation.ui.presentation.TranslationSessionExternalAction
import mihon.translation.ui.session.language.TranslationLanguageContext
import mihon.translation.ui.session.language.TranslationLanguageStore
import mihon.translation.ui.session.language.TranslationLanguageSuggestions
import mihon.translation.ui.session.language.awaitsLanguageChoice
import mihon.translation.ui.session.language.suggestedLanguages

class TranslationSessionHostCoordinator(
    feature: TranslationFeature,
    private val hostActions: TranslationHostActions,
    private val scope: CoroutineScope,
    executionMode: TranslationSessionExecutionMode = TranslationSessionExecutionMode.FollowProviderPolicy,
    selectionSettleDelayMillis: Long = 250L,
    languageStore: TranslationLanguageStore? = null,
) {
    /** Runs the coordinator's own observation, which [close] stops. */
    private val observationScope = CoroutineScope(scope.coroutineContext + SupervisorJob(scope.coroutineContext[Job]))

    private val environment = TranslationSessionEnvironmentController(hostActions, scope)
    val engineInspection = environment.inspection
    val engineStates = environment.engineStates
    val profileSelectedEngine: TranslationEngineId?
        get() = environment.inspection.value.selectedEngine

    val controller = TranslationSessionController(
        feature = feature,
        parentScope = scope,
        executionMode = executionMode,
        selectionSettleDelayMillis = selectionSettleDelayMillis,
    )
    val languageSupport: StateFlow<TranslationLanguageSupportState> = environment.languageSupport

    /** Languages every request of this session uses; picks made here update it. */
    val languages = TranslationLanguageContext(hostActions::defaultTarget, languageStore, observationScope)

    private val mutablePicker = MutableStateFlow<TranslationSessionPicker?>(null)
    val picker: StateFlow<TranslationSessionPicker?> = mutablePicker.asStateFlow()

    private val mutableResults = MutableSharedFlow<TranslationHostActionResult>(extraBufferCapacity = 1)
    val results: SharedFlow<TranslationHostActionResult> = mutableResults.asSharedFlow()

    private val mutableNewlyKeptTargets = MutableSharedFlow<LanguageTag>(extraBufferCapacity = 1)

    /** Targets the content starts keeping as its own after following the default target. */
    val newlyKeptTargets: SharedFlow<LanguageTag> = mutableNewlyKeptTargets.asSharedFlow()

    private var actionJob: Job? = null
    private var retryAfterResume = false

    /** The languages the open picker is about: the one it marks as chosen and its counterpart. */
    private var pickerLanguages = PickerLanguages()

    /** Recently chosen languages, most recent first, shared with every other translation surface. */
    val recentLanguages: StateFlow<List<LanguageTag>> = hostActions.recentLanguages.stateIn(observationScope)

    /** One-tap languages offered while the translation waits for a language choice. */
    val languageSuggestions: StateFlow<TranslationLanguageSuggestions?> = combine(
        controller.state,
        languageSupport,
        recentLanguages,
        engineStates,
    ) { state, support, recents, engines ->
        val preparation = (state as? TranslationSessionState.PreparationRequired)?.preparation
            ?: return@combine null
        val engine = activeEngine(state)
        val offered = suggestedLanguages(
            preparation = preparation,
            recentLanguages = recents,
            defaultTarget = hostActions.defaultTarget()?.language,
            support = support.of(engine),
        ) ?: return@combine null
        TranslationLanguageSuggestions(
            preparation = preparation,
            languages = offered,
            engineName = engines.firstOrNull { it.engine.id == engine }?.engine?.engineName,
        )
    }.stateIn(observationScope, SharingStarted.Eagerly, null)

    init {
        observationScope.launch {
            controller.state.collect { state ->
                val preparation = (state as? TranslationSessionState.PreparationRequired)?.preparation
                if (preparation?.awaitsLanguageChoice() == true) loadMissingLanguageSupport(activeEngine(state))
            }
        }
    }

    /** Translates [text] with the session's languages. */
    fun submit(
        text: String,
        languageContext: TextLanguageResolutionContext,
        anchor: TranslationSelectionAnchor?,
        knownSource: LanguageTag? = null,
    ) {
        controller.submit(TranslationSessionInput(languages.request(text, languageContext, knownSource), anchor))
    }

    /** Applies a source language suggested for text whose language could not be determined. */
    fun selectSuggestedSource(language: LanguageTag) {
        hostActions.recordRecentLanguage(language)
        controller.selectSource(languages.selectSource(language))
    }

    /** Applies a target language suggested for a translation that needs a different target. */
    fun selectSuggestedTarget(language: LanguageTag) {
        chooseTarget(language)
    }

    /**
     * Makes [language] the profile's default target, so content without its own target uses it. A target the
     * content kept only because it differed from the old default follows the default again.
     */
    fun makeDefaultTarget(language: LanguageTag) {
        hostActions.setDefaultTargetLanguage(language)
        if (languages.choices.value.target == language) {
            controller.selectTarget(languages.selectTarget(null))
        }
    }

    /** Applies an engine the popup offered because the chosen one cannot translate. */
    fun selectOfferedEngine(selection: TranslationEngineSelection) {
        languages.selectEngine((selection as? TranslationEngineSelection.Explicit)?.engine)
        controller.selectEngine(selection)
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

    fun selectLanguage(language: LanguageTag) {
        val available = languageSupport.value as? TranslationLanguageSupportState.Available
            ?: return
        if (available.engine != activeEngine()) return
        when (mutablePicker.value) {
            TranslationSessionPicker.SourceLanguage -> {
                if (!available.support.supportsSelection(
                        TranslationLanguageRole.Source,
                        language,
                        pickerLanguages.target,
                    )
                ) {
                    return
                }
                hostActions.recordRecentLanguage(language)
                controller.selectSource(languages.selectSource(language))
            }
            TranslationSessionPicker.TargetLanguage -> {
                if (!available.support.supportsSelection(
                        TranslationLanguageRole.Target,
                        language,
                        pickerLanguages.source,
                    )
                ) {
                    return
                }
                chooseTarget(language)
            }
            TranslationSessionPicker.Engine,
            null,
            -> return
        }
        mutablePicker.value = null
    }

    /** Makes the open picker's role follow its default again instead of a pinned language. */
    fun selectLanguageDefault() {
        when (mutablePicker.value) {
            TranslationSessionPicker.SourceLanguage -> controller.selectSource(languages.selectSource(null))
            TranslationSessionPicker.TargetLanguage -> controller.selectTarget(languages.selectTarget(null))
            TranslationSessionPicker.Engine,
            null,
            -> return
        }
        mutablePicker.value = null
    }

    /** The default the [picker] offers as its first row. */
    fun languageDefault(picker: TranslationSessionPicker): TranslationSessionLanguageDefault? {
        val request = (controller.state.value as? TranslationSessionState.Active)?.input?.request ?: return null
        return when (picker) {
            TranslationSessionPicker.SourceLanguage -> {
                val automatic = request.sourceLanguage == TranslationSourceLanguageSelection.Automatic
                TranslationSessionLanguageDefault.AutomaticSource(
                    detected = pickerLanguages.source.takeIf { automatic },
                    declared = request.languageContext.declaredLanguages.firstOrNull(),
                    selected = automatic,
                )
            }
            TranslationSessionPicker.TargetLanguage -> hostActions.defaultTarget()?.let { target ->
                TranslationSessionLanguageDefault.Target(
                    target = target,
                    selected = request.targetLanguage == TranslationTargetLanguageSelection.Default,
                )
            }
            TranslationSessionPicker.Engine -> null
        }
    }

    fun selectEngine(engine: TranslationEngineId) {
        if (engineStates.value.none { it.engine.id == engine && it.status == TranslationEngineStatus.Ready }) {
            return
        }
        languages.selectEngine(engine)
        controller.selectEngine(TranslationEngineSelection.Explicit(engine))
        environment.loadLanguageSupport(engine)
        mutablePicker.value = null
    }

    fun openEngineSetup(engine: TranslationEngineId) {
        performAction {
            hostActions.openSetup(engine)
        }
    }

    /** The pinned language the [picker] marks as chosen; none while its role follows the default row. */
    fun selectedLanguage(picker: TranslationSessionPicker): LanguageTag? =
        if (languageDefault(picker)?.selected == true) {
            null
        } else {
            when (picker) {
                TranslationSessionPicker.SourceLanguage -> pickerLanguages.source
                TranslationSessionPicker.TargetLanguage -> pickerLanguages.target
                TranslationSessionPicker.Engine -> null
            }
        }

    fun counterpartLanguage(picker: TranslationSessionPicker): LanguageTag? =
        when (picker) {
            TranslationSessionPicker.SourceLanguage -> pickerLanguages.target
            TranslationSessionPicker.TargetLanguage -> pickerLanguages.source
            TranslationSessionPicker.Engine -> null
        }

    fun activeEngine(): TranslationEngineId? = activeEngine(controller.state.value)

    private fun activeEngine(state: TranslationSessionState): TranslationEngineId? {
        val active = state as? TranslationSessionState.Active
        return when (val selection = active?.input?.request?.engine) {
            is TranslationEngineSelection.Explicit -> selection.engine
            TranslationEngineSelection.ProfileDefault,
            null,
            -> profileSelectedEngine
        }
    }

    fun retryLanguageSupport() = environment.retryLanguageSupport()

    fun loadLanguageSupport(engine: TranslationEngineId?) = environment.loadLanguageSupport(engine)

    fun dismissPicker() {
        mutablePicker.value = null
    }

    fun onResume() {
        environment.refreshEngineStates()
        if (mutablePicker.value != null && mutablePicker.value != TranslationSessionPicker.Engine) {
            environment.loadLanguageSupport(activeEngine())
        }
        if (!retryAfterResume) return
        retryAfterResume = false
        controller.retry()
    }

    fun close() {
        observationScope.cancel()
        actionJob?.cancel()
        mutablePicker.value = null
        environment.close()
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
            environment.refreshEngineStates()
        }
    }

    /** Pins [language] as the target and tells the host when the content starts keeping a target of its own. */
    private fun chooseTarget(language: LanguageTag) {
        val followedDefault = languages.choices.value.target == null
        hostActions.recordRecentLanguage(language)
        controller.selectTarget(languages.selectTarget(language))
        val kept = languages.choices.value.target
        if (languages.keepsChoices && followedDefault && kept != null) mutableNewlyKeptTargets.tryEmit(kept)
    }

    private fun loadMissingLanguageSupport(engine: TranslationEngineId?) {
        val loadedEngine = when (val support = languageSupport.value) {
            TranslationLanguageSupportState.Idle -> null
            is TranslationLanguageSupportState.Loading -> support.engine
            is TranslationLanguageSupportState.Available -> support.engine
            is TranslationLanguageSupportState.Unavailable -> support.engine
        }
        if (loadedEngine != engine) environment.loadLanguageSupport(engine)
    }

    /**
     * What [engine] can translate: its support once inspected, any language when inspection failed so the engine
     * judges each request itself, and null while it is still unknown.
     */
    private fun TranslationLanguageSupportState.of(engine: TranslationEngineId?): TranslationLanguageSupport? =
        when (this) {
            is TranslationLanguageSupportState.Available -> support.takeIf { this.engine == engine }
            is TranslationLanguageSupportState.Unavailable ->
                TranslationLanguageSupport.AnyLanguage.takeIf { this.engine == engine }
            TranslationLanguageSupportState.Idle,
            is TranslationLanguageSupportState.Loading,
            -> null
        }

    private fun openLanguagePicker(picker: TranslationSessionPicker) {
        pickerLanguages = resolvedLanguageContext()
        loadMissingLanguageSupport(activeEngine())
        mutablePicker.value = picker
    }

    private fun resolvedLanguageContext(): PickerLanguages {
        val state = controller.state.value as? TranslationSessionState.Active
            ?: return PickerLanguages()
        val explicit = PickerLanguages(
            source = (state.input.request.sourceLanguage as? TranslationSourceLanguageSelection.Explicit)?.language,
            target = (state.input.request.targetLanguage as? TranslationTargetLanguageSelection.Explicit)?.language,
        )
        val previous = state.displayedResult()?.let { PickerLanguages(it.sourceLanguage, it.targetLanguage) }
        return when (state) {
            is TranslationSessionState.Ready ->
                PickerLanguages(state.preparation.request.sourceLanguage, state.preparation.request.targetLanguage)
            is TranslationSessionState.Success,
            is TranslationSessionState.Settling,
            is TranslationSessionState.Preparing,
            is TranslationSessionState.Translating,
            -> previous ?: explicit
            is TranslationSessionState.PreparationRequired -> when (val preparation = state.preparation) {
                is TranslationPreparation.TargetLanguageRequired ->
                    explicit.copy(source = preparation.sourceLanguage ?: explicit.source)
                is TranslationPreparation.Unavailable -> {
                    val pair = preparation.reason as? TranslationUnavailableReason.UnsupportedLanguagePair
                    PickerLanguages(pair?.source ?: explicit.source, pair?.target ?: explicit.target)
                }
                else -> explicit
            }
            is TranslationSessionState.ProviderSurfaceOpened,
            is TranslationSessionState.Failed,
            -> explicit
        }
    }
}

enum class TranslationSessionPicker {
    SourceLanguage,
    TargetLanguage,
    Engine,
}

private data class PickerLanguages(
    val source: LanguageTag? = null,
    val target: LanguageTag? = null,
)
