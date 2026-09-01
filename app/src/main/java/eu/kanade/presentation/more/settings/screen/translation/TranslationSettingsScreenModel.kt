package eu.kanade.presentation.more.settings.screen.translation

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mihon.language.api.tag.LanguageTag
import mihon.translation.api.TranslationFeature
import mihon.translation.api.engine.TranslationEngineId
import mihon.translation.api.engine.TranslationEngineSelection
import mihon.translation.api.engine.TranslationEngineStatus
import mihon.translation.api.host.TranslationHostActionResult
import mihon.translation.api.host.TranslationHostActions
import mihon.translation.api.model.TranslationModelDescriptor
import mihon.translation.api.provider.TranslationProviderDisclosure
import mihon.translation.api.request.TranslationRequest
import mihon.translation.api.request.TranslationSourceLanguageSelection
import mihon.translation.api.request.TranslationTargetLanguageSelection
import mihon.translation.ui.picker.language.TranslationLanguageRole
import mihon.translation.ui.picker.language.supportsPair
import mihon.translation.ui.picker.language.supportsSelection
import mihon.translation.ui.session.TranslationLanguageSupportState
import mihon.translation.ui.session.TranslationSessionController
import mihon.translation.ui.session.TranslationSessionEnvironmentController
import mihon.translation.ui.session.TranslationSessionExecutionMode
import mihon.translation.ui.session.TranslationSessionInput
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.Locale

internal class TranslationSettingsScreenModel(
    feature: TranslationFeature = Injekt.get(),
    private val hostActions: TranslationHostActions = Injekt.get(),
) : ScreenModel {
    private val environment = TranslationSessionEnvironmentController(hostActions, screenModelScope)
    val engines = environment.engineStates
    val controller = TranslationSessionController(
        feature = feature,
        parentScope = screenModelScope,
        executionMode = TranslationSessionExecutionMode.FollowProviderPolicy,
        selectionSettleDelayMillis = PLAYGROUND_DEBOUNCE_MILLIS,
    )
    val languageSupport = environment.languageSupport
    private var savedDefaults = initialPlaygroundDefaults()
    private var environmentObservationJob: Job? = null
    private var appliedResolvedSelection = false
    private var lastResolvedSelection: TranslationEngineId? = null
    private var retryAfterSetupResume = false
    private val mutablePlayground = MutableStateFlow(initialPlaygroundState(savedDefaults))
    val playground = mutablePlayground.asStateFlow()

    init {
        observeEnvironment()
        submitPlayground()
    }

    fun setText(text: String) {
        updatePlayground { it.copy(text = text) }
    }

    fun setSourceLanguage(language: LanguageTag) {
        val current = mutablePlayground.value
        if (!isSelectable(TranslationLanguageRole.Source, language, current.targetLanguage)) return
        updatePlayground { it.copy(sourceLanguage = language) }
    }

    fun setTargetLanguage(language: LanguageTag) {
        val current = mutablePlayground.value
        if (!isSelectable(TranslationLanguageRole.Target, language, current.sourceLanguage)) return
        updatePlayground { it.copy(targetLanguage = language) }
    }

    fun swapLanguages() {
        val current = mutablePlayground.value
        if (!current.hasSupportedPair(current.targetLanguage, current.sourceLanguage)) return
        updatePlayground {
            it.copy(
                sourceLanguage = it.targetLanguage,
                targetLanguage = it.sourceLanguage,
            )
        }
    }

    fun setEngine(engine: TranslationEngineId) {
        if (engines.value.none { it.engine.id == engine && it.status == TranslationEngineStatus.Ready }) {
            return
        }
        updatePlayground {
            it.copy(
                engine = engine,
                engineSelectionResolved = true,
            )
        }
        environment.loadLanguageSupport(engine)
    }

    fun refreshEngineStates() {
        environment.refreshEngineStates()
    }

    fun onResume() {
        refreshEngineStates()
        if (retryAfterSetupResume) {
            retryAfterSetupResume = false
            controller.retry()
        }
    }

    fun supportsSetup(engine: TranslationEngineId?): Boolean =
        engine != null && hostActions.supportsSetup(engine)

    fun savePlaygroundDefaults() {
        val state = mutablePlayground.value
        if (!state.hasUnsavedProfileChanges || !state.hasSupportedPair()) return
        if (state.engine != savedDefaults.engine) {
            state.engine?.let(hostActions::setSelectedEngine)
        }
        if (state.targetLanguage != savedDefaults.targetLanguage) {
            hostActions.setDefaultTargetLanguage(state.targetLanguage)
        }
        savedDefaults = TranslationPlaygroundDefaults(
            engine = state.engine,
            targetLanguage = state.targetLanguage,
        )
        mutablePlayground.update { it.copy(hasUnsavedProfileChanges = false) }
    }

    private fun submitPlayground() {
        val state = mutablePlayground.value
        controller.submit(
            TranslationSessionInput(
                request = TranslationRequest(
                    text = state.text,
                    sourceLanguage = TranslationSourceLanguageSelection.Explicit(state.sourceLanguage),
                    targetLanguage = TranslationTargetLanguageSelection.Explicit(state.targetLanguage),
                    engine = state.engine
                        ?.let(TranslationEngineSelection::Explicit)
                        ?: TranslationEngineSelection.ProfileDefault,
                ),
            ),
        )
    }

    fun acknowledge(
        engine: TranslationEngineId,
        disclosure: TranslationProviderDisclosure,
        onComplete: (TranslationHostActionResult) -> Unit,
    ) {
        performHostAction(onComplete) {
            hostActions.acknowledgeProviderDisclosure(engine, disclosure)
        }
    }

    fun downloadModels(
        engine: TranslationEngineId,
        models: List<TranslationModelDescriptor>,
        onComplete: (TranslationHostActionResult) -> Unit,
    ) {
        performHostAction(onComplete) {
            hostActions.downloadModels(engine, models)
        }
    }

    fun openSetup(
        engine: TranslationEngineId,
        onComplete: (TranslationHostActionResult) -> Unit,
    ) {
        performHostAction(
            onComplete = { result ->
                if (result is TranslationHostActionResult.SetupOpened) {
                    retryAfterSetupResume = true
                }
                onComplete(result)
            },
        ) {
            hostActions.openSetup(engine)
        }
    }

    override fun onDispose() {
        environmentObservationJob?.cancel()
        environment.close()
        controller.close()
    }

    fun retryLanguageSupport() {
        environment.retryLanguageSupport()
    }

    private fun observeEnvironment() {
        environmentObservationJob = screenModelScope.launch {
            environment.inspection.collect { inspection ->
                if (!inspection.selectionResolved) return@collect
                if (appliedResolvedSelection && inspection.selectedEngine == lastResolvedSelection) return@collect
                appliedResolvedSelection = true
                lastResolvedSelection = inspection.selectedEngine
                val current = mutablePlayground.value
                val engineChanged = current.engineSelectionResolved && current.engine != savedDefaults.engine
                savedDefaults = savedDefaults.copy(engine = inspection.selectedEngine)
                mutablePlayground.update { state ->
                    state.copy(
                        engine = if (engineChanged) state.engine else inspection.selectedEngine,
                        engineSelectionResolved = true,
                    ).withUnsavedState()
                }
                environment.loadLanguageSupport(mutablePlayground.value.engine)
                submitPlayground()
            }
        }
    }

    private fun initialPlaygroundDefaults(): TranslationPlaygroundDefaults {
        val target = when (val selection = hostActions.defaultTargetLanguage.get()) {
            TranslationTargetLanguageSelection.Default -> effectiveUiLanguage()
            is TranslationTargetLanguageSelection.Explicit -> selection.language
        } ?: ENGLISH
        return TranslationPlaygroundDefaults(
            engine = hostActions.selectedEngine.get().takeIf { hostActions.selectedEngine.isSet() },
            targetLanguage = target,
        )
    }

    private fun initialPlaygroundState(defaults: TranslationPlaygroundDefaults): TranslationPlaygroundState {
        val target = defaults.targetLanguage
        val source = if (target.languageCode() == ENGLISH.value) FRENCH else ENGLISH
        val sample = if (source == FRENCH) FRENCH_SAMPLE else ENGLISH_SAMPLE
        return TranslationPlaygroundState(
            text = sample,
            sourceLanguage = source,
            targetLanguage = target,
            engine = defaults.engine,
            engineSelectionResolved = false,
            hasUnsavedProfileChanges = false,
        )
    }

    private fun updatePlayground(transform: (TranslationPlaygroundState) -> TranslationPlaygroundState) {
        mutablePlayground.update { current ->
            transform(current).withUnsavedState()
        }
        submitPlayground()
    }

    private fun TranslationPlaygroundState.withUnsavedState(): TranslationPlaygroundState {
        return copy(
            hasUnsavedProfileChanges =
            engine != savedDefaults.engine ||
                targetLanguage != savedDefaults.targetLanguage,
        )
    }

    private fun isSelectable(
        role: TranslationLanguageRole,
        language: LanguageTag,
        counterpart: LanguageTag,
    ): Boolean {
        val available = languageSupport.value as? TranslationLanguageSupportState.Available
            ?: return false
        if (available.engine != mutablePlayground.value.engine) return false
        return available.support.supportsSelection(role, language, counterpart)
    }

    private fun TranslationPlaygroundState.hasSupportedPair(
        source: LanguageTag = sourceLanguage,
        target: LanguageTag = targetLanguage,
    ): Boolean {
        val available = languageSupport.value as? TranslationLanguageSupportState.Available
            ?: return false
        return available.engine == engine && available.support.supportsPair(source, target)
    }

    private fun performHostAction(
        onComplete: (TranslationHostActionResult) -> Unit,
        action: suspend () -> TranslationHostActionResult,
    ) {
        screenModelScope.launch {
            val result = try {
                action()
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                TranslationHostActionResult.Failed("Unexpected translation setup failure")
            }
            onComplete(result)
        }
    }

    private companion object {
        val ENGLISH = LanguageTag.require("en")
        val FRENCH = LanguageTag.require("fr")
        const val ENGLISH_SAMPLE = "Hello, world"
        const val FRENCH_SAMPLE = "Bonjour tout le monde"
        const val PLAYGROUND_DEBOUNCE_MILLIS = 400L
    }
}

internal data class TranslationPlaygroundState(
    val text: String,
    val sourceLanguage: LanguageTag,
    val targetLanguage: LanguageTag,
    val engine: TranslationEngineId?,
    val engineSelectionResolved: Boolean,
    val hasUnsavedProfileChanges: Boolean,
)

private data class TranslationPlaygroundDefaults(
    val engine: TranslationEngineId?,
    val targetLanguage: LanguageTag,
)

private fun effectiveUiLanguage(): LanguageTag? {
    val locale = AppCompatDelegate.getApplicationLocales().get(0)
        ?: LocaleListCompat.getAdjustedDefault().get(0)
        ?: Locale.getDefault()
    return LanguageTag.parse(locale.toLanguageTag())
}

private fun LanguageTag.languageCode(): String {
    return Locale.forLanguageTag(value).language
}
