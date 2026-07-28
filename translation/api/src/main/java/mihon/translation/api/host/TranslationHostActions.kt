package mihon.translation.api

import tachiyomi.core.common.preference.Preference

/**
 * Provider-neutral host boundary shared by settings and reader surfaces.
 *
 * Provider adapters and setup registries remain internal to `translation:runtime`.
 */
interface TranslationHostActions {
    val knownEngines: List<KnownTranslationEngine>
    val selectedEngine: Preference<TranslationEngineId>
    val defaultTargetLanguage: Preference<TranslationTargetLanguageSelection>
    val automaticSelectionEnabled: Preference<Boolean>

    suspend fun deviceAvailability(): TranslationDeviceAvailability

    suspend fun inspectEngineStates(): List<TranslationEngineState>

    suspend fun acknowledgeProviderDisclosure(
        engine: TranslationEngineId,
        disclosure: TranslationProviderDisclosure,
    ): TranslationHostActionResult

    suspend fun downloadModels(
        engine: TranslationEngineId,
        models: List<TranslationModelDescriptor>,
        allowMeteredNetwork: Boolean = false,
    ): TranslationHostActionResult

    fun supportsSetup(engine: TranslationEngineId): Boolean

    suspend fun openSetup(engine: TranslationEngineId): TranslationHostActionResult

    fun setSelectedEngine(engine: TranslationEngineId)

    fun setDefaultTargetLanguage(language: TranslationLanguageTag?)
}

sealed interface TranslationHostActionResult {
    data object Completed : TranslationHostActionResult

    data object ModelsReady : TranslationHostActionResult

    data class ModelsFailed(
        val reason: String,
    ) : TranslationHostActionResult {
        init {
            require(reason.isNotBlank())
        }
    }

    data object SetupOpened : TranslationHostActionResult

    data object SetupUnsupported : TranslationHostActionResult

    data object ServiceMissing : TranslationHostActionResult

    data object SettingsUnavailable : TranslationHostActionResult

    data class Failed(
        val reason: String,
    ) : TranslationHostActionResult {
        init {
            require(reason.isNotBlank())
        }
    }
}
