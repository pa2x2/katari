package eu.kanade.tachiyomi.ui.translator.session

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mihon.language.api.tag.LanguageTag
import mihon.translation.api.TranslationFeature
import mihon.translation.api.engine.TranslationEngineId
import mihon.translation.api.engine.TranslationEngineSelection
import mihon.translation.api.engine.TranslationEngineStatus
import mihon.translation.api.host.TranslationHostActions
import mihon.translation.api.request.TranslationRequest
import mihon.translation.api.request.TranslationSourceLanguageSelection
import mihon.translation.api.request.TranslationTargetLanguageSelection
import mihon.translation.ui.picker.language.supportsPair
import mihon.translation.ui.presentation.TranslationResultSpeechPhase
import mihon.translation.ui.presentation.TranslationResultSpeechState
import mihon.translation.ui.presentation.TranslationResultSpeechTarget
import mihon.translation.ui.presentation.TranslationSessionExternalAction
import mihon.translation.ui.presentation.speechTargets
import mihon.translation.ui.session.TranslationLanguageSupportState
import mihon.translation.ui.session.TranslationSessionExecutionMode
import mihon.translation.ui.session.TranslationSessionHostCoordinator
import mihon.translation.ui.session.TranslationSessionInput
import mihon.translation.ui.session.TranslationSessionState
import mihon.translation.ui.session.displayedSessionResult
import mihon.tts.api.TtsFeature
import mihon.tts.api.request.TtsLanguageSelection
import mihon.tts.ui.playback.ShortFormSpeechController
import mihon.tts.ui.playback.ShortFormSpeechPhase
import mihon.tts.ui.playback.ShortFormSpeechRequest
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

internal class TranslatorScreenModel(
    initialText: String = "",
    feature: TranslationFeature = Injekt.get(),
    private val hostActions: TranslationHostActions = Injekt.get(),
    ttsFeature: TtsFeature = Injekt.get(),
    private val languagePreferences: TranslatorLanguagePreferences = Injekt.get(),
) : ScreenModel {
    private val coordinator = TranslationSessionHostCoordinator(
        feature = feature,
        hostActions = hostActions,
        scope = screenModelScope,
        executionMode = TranslationSessionExecutionMode.FollowProviderPolicy,
        selectionSettleDelayMillis = TRANSLATION_DEBOUNCE_MILLIS,
    )
    private val mutableState = MutableStateFlow(
        TranslatorState(
            text = initialText,
            sourceLanguage = languagePreferences.sourceLanguage.get(),
            targetLanguage = languagePreferences.targetLanguage.get(),
            defaultTarget = hostActions.defaultTarget(),
            recentLanguages = coordinator.recentLanguages.value,
            engines = coordinator.engineStates.value,
        ),
    )
    val state = mutableState.asStateFlow()
    private val eventChannel = Channel<TranslatorEvent>(capacity = Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    val controller = coordinator.controller
    private val speechController = ShortFormSpeechController<TranslationResultSpeechTarget>(
        feature = ttsFeature,
        scope = screenModelScope,
        onFailure = { eventChannel.trySend(TranslatorEvent.SpeechFailed) },
    )
    init {
        observeSession()
        observeEnvironment()
        observeSpeech()
        submit()
    }

    fun setText(text: String) {
        mutableState.update { it.copy(text = text, picker = null) }
        speechController.stopPlayback()
        if (text.isBlank()) {
            controller.clear()
            mutableState.update {
                it.copy(
                    session = TranslationSessionState.Hidden,
                )
            }
        } else {
            submit()
        }
    }

    fun clearText() = setText("")

    fun showPicker(picker: TranslatorPicker) {
        mutableState.update { it.copy(picker = picker) }
    }

    fun dismissPicker() {
        mutableState.update { it.copy(picker = null) }
    }

    fun selectAutomaticSource() = useLanguages(source = TranslationSourceLanguageSelection.Automatic)

    fun selectSource(language: LanguageTag) {
        hostActions.recordRecentLanguage(language)
        useLanguages(source = TranslationSourceLanguageSelection.Explicit(language))
    }

    fun selectDefaultTarget() = useLanguages(target = TranslationTargetLanguageSelection.Default)

    fun selectTarget(language: LanguageTag) {
        hostActions.recordRecentLanguage(language)
        useLanguages(target = TranslationTargetLanguageSelection.Explicit(language))
    }

    fun selectEngine(engine: TranslationEngineId) {
        if (mutableState.value.engines.none { it.engine.id == engine && it.status == TranslationEngineStatus.Ready }) {
            return
        }
        speechController.stopPlayback()
        mutableState.update {
            it.copy(
                engine = TranslationEngineSelection.Explicit(engine),
                picker = null,
            )
        }
        loadActiveEngineAndSubmit()
    }

    fun swapLanguages() {
        val current = mutableState.value
        val support = (current.languageSupport as? TranslationLanguageSupportState.Available)
            ?.takeIf { it.engine == current.activeEngine }
            ?.support
        val successful = current.session.displayedSessionResult()
        if (successful != null && support != null) {
            val result = successful.result
            if (support.supportsPair(result.targetLanguage, result.sourceLanguage)) {
                hostActions.recordRecentLanguage(result.sourceLanguage)
                hostActions.recordRecentLanguage(result.targetLanguage)
                useLanguages(
                    source = TranslationSourceLanguageSelection.Explicit(result.targetLanguage),
                    target = TranslationTargetLanguageSelection.Explicit(result.sourceLanguage),
                    text = result.translatedText,
                )
                return
            }
        }
        val source = current.explicitSourceLanguage
        val target = current.explicitTargetLanguage
        if (support != null && source != null && target != null && support.supportsPair(target, source)) {
            hostActions.recordRecentLanguage(source)
            hostActions.recordRecentLanguage(target)
            useLanguages(
                source = TranslationSourceLanguageSelection.Explicit(target),
                target = TranslationTargetLanguageSelection.Explicit(source),
            )
            return
        }
        eventChannel.trySend(TranslatorEvent.SwapUnavailable)
    }

    fun retry() = controller.retry()

    fun execute() = controller.execute()

    fun retryLanguageSupport() = coordinator.retryLanguageSupport()

    fun handleExternalAction(
        action: TranslationSessionExternalAction,
        openDocumentation: (String) -> Unit,
    ) {
        when (action) {
            TranslationSessionExternalAction.ChooseSourceLanguage -> showPicker(TranslatorPicker.SourceLanguage)
            TranslationSessionExternalAction.ChooseTargetLanguage -> showPicker(TranslatorPicker.TargetLanguage)
            TranslationSessionExternalAction.ChooseEngine -> showPicker(TranslatorPicker.Engine)
            is TranslationSessionExternalAction.ConfirmProviderDisclosure,
            is TranslationSessionExternalAction.DownloadModels,
            is TranslationSessionExternalAction.OpenSetup,
            is TranslationSessionExternalAction.OpenDocumentation,
            -> coordinator.handleExternalAction(action, openDocumentation)
        }
    }

    fun toggleSpeech(target: TranslationResultSpeechTarget) = speechController.toggle(
        ShortFormSpeechRequest(
            owner = target,
            text = target.text,
            language = TtsLanguageSelection.Explicit(target.language),
        ),
    )

    override fun onDispose() {
        speechController.close()
        coordinator.close()
        eventChannel.close()
    }

    private fun observeEnvironment() {
        screenModelScope.launch {
            coordinator.engineInspection.collect { inspection ->
                mutableState.update {
                    it.copy(
                        engines = inspection.engines,
                        profileEngine = inspection.selectedEngine,
                        engineSelectionResolved = inspection.selectionResolved,
                    )
                }
                if (inspection.selectionResolved) loadActiveEngineAndSubmit()
            }
        }
        screenModelScope.launch {
            coordinator.languageSupport.collect { support ->
                mutableState.update { it.copy(languageSupport = support) }
            }
        }
        screenModelScope.launch {
            coordinator.recentLanguages.collect { recents ->
                mutableState.update { it.copy(recentLanguages = recents) }
            }
        }
    }

    private fun observeSession() {
        screenModelScope.launch {
            controller.state.collect { session ->
                mutableState.update { it.copy(session = session) }
                session.displayedSessionResult()?.let {
                    speechController.stopIfOwnerChanged(it.speechTargets())
                }
            }
        }
    }

    private fun observeSpeech() {
        screenModelScope.launch {
            speechController.state.collect { speech ->
                mutableState.update {
                    it.copy(
                        speech = TranslationResultSpeechState(
                            activeTarget = speech.owner,
                            phase = when (speech.phase) {
                                ShortFormSpeechPhase.Idle -> null
                                ShortFormSpeechPhase.Preparing -> TranslationResultSpeechPhase.Preparing
                                ShortFormSpeechPhase.Speaking -> TranslationResultSpeechPhase.Speaking
                            },
                        ),
                    )
                }
            }
        }
    }

    /** Translates with these languages and keeps them for the next time the tab opens. */
    private fun useLanguages(
        source: TranslationSourceLanguageSelection = mutableState.value.sourceLanguage,
        target: TranslationTargetLanguageSelection = mutableState.value.targetLanguage,
        text: String = mutableState.value.text,
    ) {
        speechController.stopPlayback()
        mutableState.update { it.copy(text = text, sourceLanguage = source, targetLanguage = target, picker = null) }
        languagePreferences.sourceLanguage.set(source)
        languagePreferences.targetLanguage.set(target)
        submit()
    }

    private fun loadActiveEngineAndSubmit() {
        coordinator.loadLanguageSupport(mutableState.value.activeEngine)
        submit()
    }

    private fun submit() {
        val current = mutableState.value
        if (current.text.isBlank()) return
        controller.submit(
            TranslationSessionInput(
                TranslationRequest(
                    text = current.text,
                    sourceLanguage = current.sourceLanguage,
                    targetLanguage = current.targetLanguage,
                    engine = current.engine,
                ),
            ),
        )
    }

    private companion object {
        const val TRANSLATION_DEBOUNCE_MILLIS = 200L
    }
}
