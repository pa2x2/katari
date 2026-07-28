package eu.kanade.presentation.more.settings.screen.translation

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mihon.translation.api.KnownTranslationEngine
import mihon.translation.api.TranslationEngineId
import mihon.translation.api.TranslationEngineSelection
import mihon.translation.api.TranslationFeature
import mihon.translation.api.TranslationHostActionResult
import mihon.translation.api.TranslationHostActions
import mihon.translation.api.TranslationLanguageTag
import mihon.translation.api.TranslationModelDescriptor
import mihon.translation.api.TranslationProviderDisclosure
import mihon.translation.api.TranslationRequest
import mihon.translation.api.TranslationSourceLanguageSelection
import mihon.translation.api.TranslationTargetLanguageSelection
import mihon.translation.ui.session.TranslationSessionController
import mihon.translation.ui.session.TranslationSessionExecutionMode
import mihon.translation.ui.session.TranslationSessionInput
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.Locale

internal class TranslationSettingsScreenModel(
    feature: TranslationFeature = Injekt.get(),
    private val hostActions: TranslationHostActions = Injekt.get(),
) : ScreenModel {
    val engines: List<KnownTranslationEngine> = hostActions.knownEngines
    val controller = TranslationSessionController(
        feature = feature,
        parentScope = screenModelScope,
        executionMode = TranslationSessionExecutionMode.FollowProviderPolicy,
        selectionSettleDelayMillis = PLAYGROUND_DEBOUNCE_MILLIS,
    )
    private var savedDefaults = initialPlaygroundDefaults()
    private val mutablePlayground = MutableStateFlow(initialPlaygroundState(savedDefaults))
    val playground = mutablePlayground.asStateFlow()

    init {
        submitPlayground()
    }

    fun setText(text: String) {
        updatePlayground { it.copy(text = text) }
    }

    fun setSourceLanguage(language: TranslationLanguageTag) {
        updatePlayground { it.copy(sourceLanguage = language) }
    }

    fun setTargetLanguage(language: TranslationLanguageTag) {
        updatePlayground { it.copy(targetLanguage = language) }
    }

    fun swapLanguages() {
        updatePlayground {
            it.copy(
                sourceLanguage = it.targetLanguage,
                targetLanguage = it.sourceLanguage,
            )
        }
    }

    fun setEngine(engine: TranslationEngineId) {
        updatePlayground { it.copy(engine = engine) }
    }

    fun supportsSystemSetup(engine: TranslationEngineId): Boolean =
        hostActions.supportsSystemSetup(engine)

    fun savePlaygroundDefaults() {
        val state = mutablePlayground.value
        if (!state.hasUnsavedProfileChanges) return
        hostActions.setSelectedEngine(state.engine)
        hostActions.setDefaultTargetLanguage(state.targetLanguage)
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
                    engine = TranslationEngineSelection.Explicit(state.engine),
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

    fun openSystemSetup(
        engine: TranslationEngineId,
        onComplete: (TranslationHostActionResult) -> Unit,
    ) {
        performHostAction(onComplete) {
            hostActions.openSystemSetup(engine)
        }
    }

    override fun onDispose() {
        controller.close()
    }

    private fun initialPlaygroundDefaults(): TranslationPlaygroundDefaults {
        val target = when (val selection = hostActions.defaultTargetLanguage.get()) {
            TranslationTargetLanguageSelection.Default -> effectiveUiLanguage()
            is TranslationTargetLanguageSelection.Explicit -> selection.language
        } ?: ENGLISH
        return TranslationPlaygroundDefaults(
            engine = hostActions.selectedEngine.get(),
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
            hasUnsavedProfileChanges = false,
        )
    }

    private fun updatePlayground(transform: (TranslationPlaygroundState) -> TranslationPlaygroundState) {
        mutablePlayground.update { current ->
            transform(current).let { updated ->
                updated.copy(
                    hasUnsavedProfileChanges =
                    updated.engine != savedDefaults.engine ||
                        updated.targetLanguage != savedDefaults.targetLanguage,
                )
            }
        }
        submitPlayground()
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
        val ENGLISH = TranslationLanguageTag.require("en")
        val FRENCH = TranslationLanguageTag.require("fr")
        const val ENGLISH_SAMPLE = "Hello, world"
        const val FRENCH_SAMPLE = "Bonjour tout le monde"
        const val PLAYGROUND_DEBOUNCE_MILLIS = 400L
    }
}

internal data class TranslationPlaygroundState(
    val text: String,
    val sourceLanguage: TranslationLanguageTag,
    val targetLanguage: TranslationLanguageTag,
    val engine: TranslationEngineId,
    val hasUnsavedProfileChanges: Boolean,
)

private data class TranslationPlaygroundDefaults(
    val engine: TranslationEngineId,
    val targetLanguage: TranslationLanguageTag,
)

private fun effectiveUiLanguage(): TranslationLanguageTag? {
    val locale = AppCompatDelegate.getApplicationLocales().get(0)
        ?: LocaleListCompat.getAdjustedDefault().get(0)
        ?: Locale.getDefault()
    return TranslationLanguageTag.parse(locale.toLanguageTag())
}

private fun TranslationLanguageTag.languageCode(): String {
    return Locale.forLanguageTag(value).language
}
