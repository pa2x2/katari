package eu.kanade.presentation.more.settings.screen.translation

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import mihon.translation.api.KnownTranslationEngine
import mihon.translation.api.TranslationEngineId
import mihon.translation.api.TranslationFeature
import mihon.translation.api.TranslationLanguageTag
import mihon.translation.api.TranslationModelDescriptor
import mihon.translation.api.TranslationModelOperationResult
import mihon.translation.api.TranslationProviderDisclosure
import mihon.translation.api.TranslationRequest
import mihon.translation.api.TranslationTargetLanguageSelection
import mihon.translation.runtime.ProfileTranslationPreferences
import mihon.translation.spi.KnownTranslationEngineCatalog
import mihon.translation.spi.TranslationEngineSetupRegistry
import mihon.translation.spi.TranslationSystemSetupResult
import mihon.translation.ui.session.TranslationSessionController
import mihon.translation.ui.session.TranslationSessionInput
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

internal class TranslationSettingsScreenModel(
    feature: TranslationFeature = Injekt.get(),
    val preferences: ProfileTranslationPreferences = Injekt.get(),
    knownEngineCatalog: KnownTranslationEngineCatalog = Injekt.get(),
    private val setupRegistry: TranslationEngineSetupRegistry = Injekt.get(),
) : ScreenModel {
    val engines: List<KnownTranslationEngine> = knownEngineCatalog.knownEngines
    val controller = TranslationSessionController(feature, screenModelScope)

    fun submitTest(text: String) {
        controller.submit(
            TranslationSessionInput(
                request = TranslationRequest(
                    text = text,
                    targetLanguage = preferences.targetLanguage.get(),
                    engine = preferences.engineSelection.get(),
                ),
            ),
        )
    }

    fun setDefaultTarget(language: TranslationLanguageTag?) {
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
