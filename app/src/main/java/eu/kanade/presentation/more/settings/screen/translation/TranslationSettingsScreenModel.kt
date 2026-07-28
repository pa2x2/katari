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
import mihon.translation.api.TranslationLanguageTag
import mihon.translation.api.TranslationModelDescriptor
import mihon.translation.api.TranslationModelOperationResult
import mihon.translation.api.TranslationProviderDisclosure
import mihon.translation.api.TranslationRequest
import mihon.translation.api.TranslationSourceLanguageSelection
import mihon.translation.api.TranslationTargetLanguageSelection
import mihon.translation.runtime.ProfileTranslationPreferences
import mihon.translation.spi.KnownTranslationEngineCatalog
import mihon.translation.spi.TranslationEngineSetupRegistry
import mihon.translation.spi.TranslationSystemSetupResult
import mihon.translation.ui.session.TranslationSessionController
import mihon.translation.ui.session.TranslationSessionExecutionMode
import mihon.translation.ui.session.TranslationSessionInput
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.Locale

internal class TranslationSettingsScreenModel(
    feature: TranslationFeature = Injekt.get(),
    val preferences: ProfileTranslationPreferences = Injekt.get(),
    knownEngineCatalog: KnownTranslationEngineCatalog = Injekt.get(),
    private val setupRegistry: TranslationEngineSetupRegistry = Injekt.get(),
) : ScreenModel {
    val engines: List<KnownTranslationEngine> = knownEngineCatalog.knownEngines
    val controller = TranslationSessionController(
        feature = feature,
        parentScope = screenModelScope,
        executionMode = TranslationSessionExecutionMode.FollowProviderPolicy,
        selectionSettleDelayMillis = PLAYGROUND_DEBOUNCE_MILLIS,
    )
    private val mutablePlayground = MutableStateFlow(initialPlaygroundState())
    val playground = mutablePlayground.asStateFlow()

    init {
        submitPlayground()
    }

    fun setText(text: String) {
        mutablePlayground.update { it.copy(text = text) }
        submitPlayground()
    }

    fun setSourceLanguage(language: TranslationLanguageTag) {
        mutablePlayground.update { it.copy(sourceLanguage = language) }
        submitPlayground()
    }

    fun setTargetLanguage(language: TranslationLanguageTag) {
        mutablePlayground.update { it.copy(targetLanguage = language) }
        submitPlayground()
    }

    fun swapLanguages() {
        mutablePlayground.update {
            it.copy(
                sourceLanguage = it.targetLanguage,
                targetLanguage = it.sourceLanguage,
            )
        }
        submitPlayground()
    }

    fun setEngine(engine: TranslationEngineId) {
        mutablePlayground.update { it.copy(engine = engine) }
        submitPlayground()
    }

    fun usePlaygroundEngineAsDefault() {
        preferences.engine.set(mutablePlayground.value.engine)
    }

    fun setProfileEngine(engine: TranslationEngineId) {
        preferences.engine.set(engine)
        setEngine(engine)
    }

    fun setProfileTarget(language: TranslationLanguageTag?) {
        setDefaultTarget(language)
        val resolved = language ?: effectiveUiLanguage() ?: return
        setTargetLanguage(resolved)
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

    private fun setDefaultTarget(language: TranslationLanguageTag?) {
        preferences.targetLanguage.set(
            language?.let(TranslationTargetLanguageSelection::Explicit)
                ?: TranslationTargetLanguageSelection.Default,
        )
    }

    fun acknowledge(
        engine: TranslationEngineId,
        disclosure: TranslationProviderDisclosure,
        onComplete: (TranslationHostActionResult) -> Unit,
    ) {
        performSetupAction(
            engine = engine,
            onComplete = onComplete,
        ) {
            acknowledge(disclosure)
            TranslationHostActionResult.Completed
        }
    }

    fun downloadModels(
        engine: TranslationEngineId,
        models: List<TranslationModelDescriptor>,
        onComplete: (TranslationHostActionResult) -> Unit,
    ) {
        performSetupAction(
            engine = engine,
            onComplete = onComplete,
        ) {
            when (
                val result = downloadModels(
                    models = models.mapTo(mutableSetOf()) { it.id },
                    allowMeteredNetwork = false,
                )
            ) {
                TranslationModelOperationResult.Completed -> TranslationHostActionResult.ModelsReady
                is TranslationModelOperationResult.Failed -> TranslationHostActionResult.ModelsFailed(result.reason)
            }
        }
    }

    fun openSystemSetup(
        engine: TranslationEngineId,
        onComplete: (TranslationHostActionResult) -> Unit,
    ) {
        performSetupAction(
            engine = engine,
            onComplete = onComplete,
        ) {
            when (val result = openSystemSetup()) {
                TranslationSystemSetupResult.Opened -> TranslationHostActionResult.SystemSetupOpened
                TranslationSystemSetupResult.Unsupported -> TranslationHostActionResult.SetupUnsupported
                TranslationSystemSetupResult.ServiceMissing -> TranslationHostActionResult.ServiceMissing
                TranslationSystemSetupResult.SettingsUnavailable -> TranslationHostActionResult.SettingsUnavailable
                is TranslationSystemSetupResult.Failed -> TranslationHostActionResult.Failed(result.reason)
            }
        }
    }

    override fun onDispose() {
        controller.close()
    }

    private fun initialPlaygroundState(): TranslationPlaygroundState {
        val target = when (val selection = preferences.targetLanguage.get()) {
            TranslationTargetLanguageSelection.Default -> effectiveUiLanguage()
            is TranslationTargetLanguageSelection.Explicit -> selection.language
        } ?: ENGLISH
        val source = if (target.languageCode() == ENGLISH.value) FRENCH else ENGLISH
        val sample = if (source == FRENCH) FRENCH_SAMPLE else ENGLISH_SAMPLE
        return TranslationPlaygroundState(
            text = sample,
            sourceLanguage = source,
            targetLanguage = target,
            engine = preferences.engine.get(),
        )
    }

    private fun performSetupAction(
        engine: TranslationEngineId,
        onComplete: (TranslationHostActionResult) -> Unit,
        action: suspend mihon.translation.spi.TranslationEngineSetup.() -> TranslationHostActionResult,
    ) {
        val setup = setupRegistry.find(engine)
        if (setup == null) {
            onComplete(TranslationHostActionResult.SetupUnsupported)
            return
        }
        screenModelScope.launch {
            val result = try {
                setup.action()
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

internal sealed interface TranslationHostActionResult {
    data object Completed : TranslationHostActionResult

    data object ModelsReady : TranslationHostActionResult

    data class ModelsFailed(
        val reason: String,
    ) : TranslationHostActionResult

    data object SystemSetupOpened : TranslationHostActionResult

    data object SetupUnsupported : TranslationHostActionResult

    data object ServiceMissing : TranslationHostActionResult

    data object SettingsUnavailable : TranslationHostActionResult

    data class Failed(
        val reason: String,
    ) : TranslationHostActionResult
}
