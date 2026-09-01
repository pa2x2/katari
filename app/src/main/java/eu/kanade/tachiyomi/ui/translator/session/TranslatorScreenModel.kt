package eu.kanade.tachiyomi.ui.translator.session

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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
import mihon.translation.ui.presentation.TranslationResultSpeechSide
import mihon.translation.ui.presentation.TranslationResultSpeechState
import mihon.translation.ui.presentation.TranslationResultSpeechTarget
import mihon.translation.ui.presentation.TranslationSessionExternalAction
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
import java.util.Locale

internal class TranslatorScreenModel(
    initialText: String = "",
    feature: TranslationFeature = Injekt.get(),
    private val hostActions: TranslationHostActions = Injekt.get(),
    ttsFeature: TtsFeature = Injekt.get(),
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
            profileTargetLanguage = resolveProfileTargetLanguage(hostActions),
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

    fun selectAutomaticSource() {
        speechController.stopPlayback()
        mutableState.update {
            it.copy(
                sourceLanguage = TranslationSourceLanguageSelection.Automatic,
                picker = null,
            )
        }
        submit()
    }

    fun selectSource(language: mihon.language.api.tag.LanguageTag) {
        speechController.stopPlayback()
        mutableState.update {
            it.copy(
                sourceLanguage = TranslationSourceLanguageSelection.Explicit(language),
                picker = null,
            )
        }
        submit()
    }

    fun selectTarget(language: mihon.language.api.tag.LanguageTag) {
        speechController.stopPlayback()
        mutableState.update {
            it.copy(
                targetLanguage = TranslationTargetLanguageSelection.Explicit(language),
                picker = null,
            )
        }
        submit()
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
        val successful = current.session.displayedSessionResult() ?: return
        val support = (current.languageSupport as? TranslationLanguageSupportState.Available)
            ?.takeIf { it.engine == current.activeEngine }
            ?.support
            ?: return
        val result = successful.result
        if (!support.supportsPair(result.targetLanguage, result.sourceLanguage)) return
        speechController.stopPlayback()
        mutableState.update {
            it.copy(
                text = result.translatedText,
                sourceLanguage = TranslationSourceLanguageSelection.Explicit(result.targetLanguage),
                targetLanguage = TranslationTargetLanguageSelection.Explicit(result.sourceLanguage),
                picker = null,
            )
        }
        submit()
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
            is TranslationSessionExternalAction.ChangeLanguages -> {
                mutableState.update {
                    it.copy(
                        sourceLanguage = TranslationSourceLanguageSelection.Explicit(action.source),
                        targetLanguage = TranslationTargetLanguageSelection.Explicit(action.target),
                        picker = TranslatorPicker.SourceLanguage,
                    )
                }
            }
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
    }

    private fun observeSession() {
        screenModelScope.launch {
            controller.state.collect { session ->
                mutableState.update { it.copy(session = session) }
                validSpeechTargets(session.displayedSessionResult())?.let {
                    speechController.stopIfOwnerChanged(it)
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

    private fun validSpeechTargets(
        successful: mihon.translation.ui.session.TranslationSessionResult?,
    ): Set<TranslationResultSpeechTarget>? {
        successful ?: return null
        return setOf(
            TranslationResultSpeechTarget(
                side = TranslationResultSpeechSide.Source,
                text = successful.input.request.text,
                language = successful.result.sourceLanguage,
            ),
            TranslationResultSpeechTarget(
                side = TranslationResultSpeechSide.Target,
                text = successful.result.translatedText,
                language = successful.result.targetLanguage,
            ),
        )
    }

    private companion object {
        const val TRANSLATION_DEBOUNCE_MILLIS = 200L
    }
}

private fun resolveProfileTargetLanguage(hostActions: TranslationHostActions): mihon.language.api.tag.LanguageTag? {
    return when (val target = hostActions.defaultTargetLanguage.get()) {
        TranslationTargetLanguageSelection.Default -> {
            val locale = AppCompatDelegate.getApplicationLocales().get(0)
                ?: LocaleListCompat.getAdjustedDefault().get(0)
                ?: Locale.getDefault()
            mihon.language.api.tag.LanguageTag.parse(locale.toLanguageTag())
        }
        is TranslationTargetLanguageSelection.Explicit -> target.language
    }
}
