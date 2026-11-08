package eu.kanade.tachiyomi.ui.translator.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.katari.language.api.tag.LanguageTag
import app.katari.translation.api.TranslationFeature
import app.katari.translation.api.engine.TranslationEngineId
import app.katari.translation.api.engine.TranslationEngineSelection
import app.katari.translation.api.engine.TranslationEngineStatus
import app.katari.translation.api.host.TranslationHostActions
import app.katari.translation.api.request.TranslationRequest
import app.katari.translation.api.request.TranslationSourceLanguageSelection
import app.katari.translation.api.request.TranslationTargetLanguageSelection
import app.katari.translation.ui.picker.language.supportsPair
import app.katari.translation.ui.presentation.TranslationResultSpeechPhase
import app.katari.translation.ui.presentation.TranslationResultSpeechState
import app.katari.translation.ui.presentation.TranslationResultSpeechTarget
import app.katari.translation.ui.presentation.TranslationSessionExternalAction
import app.katari.translation.ui.presentation.speechTargets
import app.katari.translation.ui.session.TranslationLanguageSupportState
import app.katari.translation.ui.session.TranslationSessionExecutionMode
import app.katari.translation.ui.session.TranslationSessionHostCoordinator
import app.katari.translation.ui.session.TranslationSessionInput
import app.katari.translation.ui.session.TranslationSessionState
import app.katari.translation.ui.session.displayedSessionResult
import app.katari.tts.api.TtsFeature
import app.katari.tts.api.request.TtsLanguageSelection
import app.katari.tts.ui.playback.ShortFormSpeechController
import app.katari.tts.ui.playback.ShortFormSpeechPhase
import app.katari.tts.ui.playback.ShortFormSpeechRequest
import app.katari.ui.di.AppUiScope
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactoryKey
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@AssistedInject
internal class TranslatorViewModel(
    @Assisted initialText: String,
    feature: TranslationFeature,
    private val hostActions: TranslationHostActions,
    ttsFeature: TtsFeature,
    private val languagePreferences: TranslatorLanguagePreferences,
) : ViewModel() {

    @AssistedFactory
    @ManualViewModelAssistedFactoryKey
    @ContributesIntoMap(AppUiScope::class)
    interface Factory : ManualViewModelAssistedFactory {
        fun create(initialText: String): TranslatorViewModel
    }

    private val coordinator = TranslationSessionHostCoordinator(
        feature = feature,
        hostActions = hostActions,
        scope = viewModelScope,
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
        scope = viewModelScope,
        onFailure = { eventChannel.trySend(TranslatorEvent.SpeechFailed(it.explanation)) },
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

    override fun onCleared() {
        speechController.close()
        coordinator.close()
        eventChannel.close()
    }

    private fun observeEnvironment() {
        viewModelScope.launch {
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
        viewModelScope.launch {
            coordinator.languageSupport.collect { support ->
                mutableState.update { it.copy(languageSupport = support) }
            }
        }
        viewModelScope.launch {
            coordinator.recentLanguages.collect { recents ->
                mutableState.update { it.copy(recentLanguages = recents) }
            }
        }
    }

    private fun observeSession() {
        viewModelScope.launch {
            controller.state.collect { session ->
                mutableState.update { it.copy(session = session) }
                session.displayedSessionResult()?.let {
                    speechController.stopIfOwnerChanged(it.speechTargets())
                }
            }
        }
    }

    private fun observeSpeech() {
        viewModelScope.launch {
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
