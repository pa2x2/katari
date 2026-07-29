package mihon.translation.api

import tachiyomi.core.common.preference.Preference

/**
 * Provider-neutral host boundary shared by settings and reader surfaces.
 *
 * Provider adapters and setup registries remain internal to `translation:runtime`.
 */
interface TranslationHostActions {
    val knownEngines: List<KnownTranslationEngine>

    /**
     * Raw profile preference. [Preference.isSet] distinguishes an explicit choice from the implicit device default.
     */
    val selectedEngine: Preference<TranslationEngineId>
    val defaultTargetLanguage: Preference<TranslationTargetLanguageSelection>

    suspend fun deviceAvailability(): TranslationDeviceAvailability

    suspend fun inspectEngines(): TranslationEngineInspection

    suspend fun inspectLanguageSupport(
        engine: TranslationEngineId,
    ): TranslationLanguageSupportInspection

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

    data class SetupOpened(
        val destination: TranslationSetupDestination,
    ) : TranslationHostActionResult

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

enum class TranslationSetupDestination {
    InApp,
    External,
}
